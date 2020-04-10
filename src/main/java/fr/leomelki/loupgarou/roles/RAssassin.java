package fr.leomelki.loupgarou.roles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.events.LGEndCheckEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGNightEndEvent;
import fr.leomelki.loupgarou.events.LGNightPlayerPreKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGPyromaneGasoilEvent;
import fr.leomelki.loupgarou.events.LGRoleTurnEndEvent;

public class RAssassin extends Role {
    public RAssassin(LGGame game, int amount) {
        super(game, amount);
    }

    @Override
    public RoleType getType() {
        return RoleType.NEUTRAL;
    }

    @Override
    public RoleWinType getWinType() {
        return RoleWinType.SEUL;
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
                    getGame().kill(choosen, Reason.ASSASSIN);
                    player.sendActionBarRoleFormat(RAssassin.this, "chooseactionbar", choosen.getName());
                    player.sendRoleFormat(RAssassin.this, "choosemessage", choosen.getName());
                    player.stopChoosing();
                    player.hideView();
                    callback.run();
                }
            }
        });
    }

    @EventHandler
    public void onKill(LGNightPlayerPreKilledEvent e) {
        if(e.getKilled().getRole() == this && e.getReason() == Reason.LOUP_GAROU || e.getReason() == Reason.GM_LOUP_GAROU) {// Les assassins ne peuvent pas mourir la nuit !
            e.setReason(Reason.DONT_DIE);
            e.getKilled().getCache().set("assassin_protected", true);
        }
    }

    @EventHandler
    public void onTour(LGRoleTurnEndEvent e) {
        if(e.getGame() == getGame()) {
            if(e.getPreviousRole() instanceof RLoupGarou) {
                for(LGPlayer lgp : getGame().getAlive())
                    if(lgp.getCache().getBoolean("assassin_protected")) {
                        for(LGPlayer l : getGame().getInGame())
                            if(l.getRoleType() == RoleType.LOUP_GAROU)
                                l.sendFormat("role.generic.targetimmune");
                    }
            } else if(e.getPreviousRole() instanceof RGrandMechantLoup) {
                for(LGPlayer lgp : getGame().getAlive())
                    if(lgp.getCache().getBoolean("assassin_protected")) {
                        for(LGPlayer l : e.getPreviousRole().getPlayers())
                            l.sendFormat("role.generic.targetimmune");
                    }
            }
        }
    }

    @EventHandler
    public void onPyroGasoil(LGPyromaneGasoilEvent e) {
        if(e.getPlayer().getRole() == this)
            e.setCancelled(true);
    }

    @EventHandler
    public void onDayStart(LGNightEndEvent e) {
        if(e.getGame() == getGame()) {
            for(LGPlayer lgp : getGame().getAlive())
                if(lgp.getCache().getBoolean("assassin_protected"))
                    lgp.getCache().remove("assassin_protected");
        }
    }

    @EventHandler
    public void onEndgameCheck(LGEndCheckEvent e) {
        if(e.getGame() == getGame() && e.getWinType() == LGWinType.SOLO) {
            if(getPlayers().size() > 0)
                e.setWinType(LGWinType.ASSASSIN);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEndGame(LGGameEndEvent e) {
        if(e.getWinType() == LGWinType.ASSASSIN) {
            e.getWinners().clear();
            e.getWinners().addAll(getPlayers());
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
