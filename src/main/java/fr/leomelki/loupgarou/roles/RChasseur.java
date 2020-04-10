package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGDayStartEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGNightStart;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.localization.Translate;

public class RChasseur extends Role {
    public RChasseur(LGGame game, int amount) {
        super(game, amount);
    }

    @Override
    public RoleType getType() {
        return RoleType.VILLAGER;
    }

    @Override
    public RoleWinType getWinType() {
        return RoleWinType.VILLAGE;
    }

    @Override
    public int getTimeout() {
        return 15;
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        getGame().wait(getTimeout(), () -> {
            this.onNightTurnTimeout(player);
            callback.run();
        }, (currentPlayer, secondsLeft) -> {
            return currentPlayer == player ? Translate.get(currentPlayer, "role.generic.yourturn") : roleFormat(currentPlayer, "othersturn");
        });
        getGame().broadcastFunction(lgp -> "§9" + getBroadcastedTask(lgp));
        player.sendMessage("§6" + getTask(player));
        // player.sendTitle("§6C'est à vous de jouer", "§a"+getTask(), 60);
        player.choose((choosen) -> {
            if(choosen != null) {
                player.stopChoosing();
                getGame().cancelWait();
                LGPlayerKilledEvent killEvent = new LGPlayerKilledEvent(getGame(), choosen, Reason.CHASSEUR);
                Bukkit.getPluginManager().callEvent(killEvent);
                if(killEvent.isCancelled())
                    return;

                if(getGame().kill(killEvent.getKilled(), killEvent.getReason(), true))
                    return;
                callback.run();
            }
        }, player);
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        getGame().broadcastRoleFormat(this, "timeout");
        player.stopChoosing();
    }

    ArrayList<LGPlayer> needToPlay = new ArrayList<LGPlayer>();

    @EventHandler
    public void onPlayerKill(LGPlayerKilledEvent e) {
        if(e.getKilled().getRole() == this && e.getReason() != Reason.DISCONNECTED)
            needToPlay.add(e.getKilled());
    }

    @EventHandler
    public void onDayStart(LGDayStartEvent e) {
        if(e.getGame() != getGame())
            return;

        if(needToPlay.size() > 0)
            e.setCancelled(true);

        if(!e.isCancelled())
            return;
        new Runnable() {
            public void run() {
                if(needToPlay.size() == 0) {
                    e.getGame().startDay();
                    return;
                }
                LGPlayer player = needToPlay.remove(0);
                onNightTurn(player, this);
            }
        }.run();
    }

    @EventHandler
    public void onEndGame(LGGameEndEvent e) {
        if(e.getGame() != getGame())
            return;

        if(needToPlay.size() > 0)
            e.setCancelled(true);

        if(!e.isCancelled())
            return;
        new Runnable() {
            public void run() {
                if(needToPlay.size() == 0) {
                    e.getGame().checkEndGame(true);
                    return;
                }
                LGPlayer player = needToPlay.remove(0);
                onNightTurn(player, this);
            }
        }.run();
    }

    /*
     * Deprecated by #onDayStart(LGDayStartEvent)
     * 
     * @EventHandler public void onVote(LGVoteEvent e) { if(e.getGame() ==
     * getGame()) { if(needToPlay.size() > 0) { e.setCancelled(true); new Runnable()
     * { public void run() { if(needToPlay.size() == 0) { e.getGame().nextNight();
     * return; } LGPlayer player = needToPlay.remove(0); onNightTurn(player, this);
     * } }.run(); } } }
     */

    @EventHandler
    public void onNight(LGNightStart e) {
        if(e.getGame() == getGame() && !e.isCancelled()) {
            if(needToPlay.size() > 0) {
                e.setCancelled(true);
                new Runnable() {
                    public void run() {
                        if(needToPlay.size() == 0) {
                            e.getGame().nextNight();
                            return;
                        }
                        LGPlayer player = needToPlay.remove(0);
                        onNightTurn(player, this);
                    }
                }.run();
            }
        }
    }
}
