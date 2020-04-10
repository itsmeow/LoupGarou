package fr.leomelki.loupgarou.roles;

import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitRunnable;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGDayEndEvent;
import fr.leomelki.loupgarou.events.LGVoteEvent;

public class RCorbeau extends Role {
    public RCorbeau(LGGame game, int amount) {
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
        player.showView();

        player.choose(new LGChooseCallback() {
            @Override
            public void callback(LGPlayer choosen) {
                if(choosen != null && choosen != player) {
                    // player.sendTitle("§6Vous avez regardé un rôle",
                    // "§e§l"+choosen.getName()+"§6§l est §e§l"+choosen.getRole().getName(), 5*20);

                    choosen.getCache().set("corbeau_selected", true);

                    player.sendActionBarRoleFormat(RCorbeau.this, "choose.actionbar", choosen.getName());
                    player.sendRoleFormat(RCorbeau.this, "choose.message", choosen.getName());
                    player.stopChoosing();
                    player.hideView();
                    callback.run();
                }
            }
        });
    }

    @EventHandler
    public void onNightStart(LGDayEndEvent e) {
        if(e.getGame() == getGame())
            for(LGPlayer lgp : getGame().getAlive())
                lgp.getCache().remove("corbeau_selected");
    }

    @EventHandler
    public void onVoteStart(LGVoteEvent e) {
        if(e.getGame() == getGame())
            for(LGPlayer lgp : getGame().getAlive())
                if(lgp.getCache().getBoolean("corbeau_selected")) {
                    lgp.getCache().remove("corbeau_selected");
                    final LGPlayer lgCap = lgp;
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            getGame().getVote().voteNamed(lg -> roleFormat(lg, "broadcastvotingname"), new LGPlayer("§a§lLe corbeau"), lgCap);
                            getGame().getVote().voteNamed(lg -> roleFormat(lg, "broadcastvotingname"), new LGPlayer("§a§lLe corbeau"), lgCap);// fix
                            getGame().broadcastFunction(lg -> roleFormat(lg, "votebroadcast", lgCap.getName(), getName(lg)));
                        }
                    }.runTask(MainLg.getInstance());

                }
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.stopChoosing();
        player.hideView();
        // player.sendTitle("§cVous n'avez regardé aucun rôle", "§4Vous avez mis trop de
        // temps à vous décider...", 80);
        // player.sendMessage("§cVous n'avez pas utilisé votre pouvoir cette nuit.");
    }
}
