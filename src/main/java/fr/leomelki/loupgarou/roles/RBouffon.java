package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitRunnable;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.localization.Translate;

public class RBouffon extends Role {

    private ArrayList<LGPlayer> needToPlay = new ArrayList<LGPlayer>();

    public RBouffon(LGGame game, int amount) {
        super(game, amount);
    }

    @Override
    public RoleType getType() {
        return RoleType.NEUTRAL;
    }

    @Override
    public RoleWinType getWinType() {
        return RoleWinType.NONE;
    }

    @Override
    public int getTimeout() {
        return 15;
    }

    @Override
    public void onNightTurn(Runnable callback) {
        @SuppressWarnings("unchecked")
        ArrayList<LGPlayer> players = (ArrayList<LGPlayer>) needToPlay.clone();
        new Runnable() {

            @Override
            public void run() {
                getGame().cancelWait();
                if(players.size() == 0) {
                    onTurnFinish(callback);
                    return;
                }
                LGPlayer player = players.remove(0);
                getGame().wait(getTimeout(), () -> {
                    RBouffon.this.onNightTurnTimeout(player);
                    this.run();
                }, (currentPlayer, secondsLeft) -> {
                    return currentPlayer == player ? Translate.get(currentPlayer, "role.generic.yourturn") : Translate.get(currentPlayer, "role.generic.othersturn", getFriendlyName(currentPlayer), secondsLeft);
                });
                player.sendMessage("§6" + getTask(player));
                // player.sendTitle("§6C'est à vous de jouer", "§a"+getTask(), 100);
                onNightTurn(player, this);
            }
        }.run();
    }

    public boolean hasPlayersLeft() {
        return needToPlay.size() > 0;
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        needToPlay.remove(player);
        player.showView();
        player.getCache().set("bouffon_win", true);
        List<LGPlayer> choosable = getGame().getVote().getVotes(player);
        StringJoiner sj = new StringJoiner(roleFormat(player, "votelist.seperator"));
        for(LGPlayer lgp : choosable)
            if(lgp.getPlayer() != null && lgp.getPlayer() != player)
                sj.add(lgp.getName());

        player.sendRoleFormat(this, "votelist", sj, sj.length());

        player.choose((choosen) -> {
            if(choosen != null) {
                if(!choosable.contains(choosen))
                    player.sendRoleFormat(this, "choose.novote", choosen.getName());
                else if(choosen.isDead())
                    player.sendRoleFormat(this, "choose.dead", choosen.getName());// fix
                else {
                    player.stopChoosing();
                    player.sendRoleFormat(this, "choose.haunt", choosen.getName());
                    getGame().kill(choosen, Reason.BOUFFON);
                    player.hideView();
                    callback.run();
                }
            }
        }, player);
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.stopChoosing();
    }

    @EventHandler
    public void onPlayerKill(LGPlayerKilledEvent e) {
        if(e.getKilled().getRole() == this && e.getReason() == Reason.VOTE) {
            needToPlay.add(e.getKilled());
            getGame().broadcastFunction(lgp -> roleFormat(lgp, "death.broadcast", getName(lgp)));
            e.getKilled().sendRoleFormat(this, "death.message");
        }
    }

    @EventHandler
    public void onWin(LGGameEndEvent e) {
        if(e.getGame() == getGame()) {
            for(LGPlayer lgp : getGame().getInGame()) {
                if(lgp.getRole() == this && lgp.getCache().getBoolean("bouffon_win")) {
                    e.getWinners().add(lgp);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            getGame().broadcastFunction(lgp -> roleFormat(lgp, "win", getName(lgp)));
                        }
                    }.runTaskAsynchronously(MainLg.getInstance());
                }
            }
        }
    }
}
