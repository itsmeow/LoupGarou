package fr.leomelki.loupgarou.roles;

import java.util.Random;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent;

public class REnfantSauvage extends Role {
    private static Random random = new Random();

    public REnfantSauvage(LGGame game, int amount) {
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
        player.sendRoleFormat(this, "model.choose");
        player.choose(new LGChooseCallback() {

            @Override
            public void callback(LGPlayer choosen) {
                if(choosen != null) {
                    player.stopChoosing();
                    player.sendRoleFormat(REnfantSauvage.this, "model.info.message", choosen.getName());
                    player.sendActionBarRoleFormat(REnfantSauvage.this, "model.info.actionbar", choosen.getName());
                    player.getCache().set("enfant_svg", choosen);
                    choosen.getCache().set("enfant_svg_d", player);
                    getPlayers().remove(player);// Pour éviter qu'il puisse avoir plusieurs modèles
                    player.hideView();
                    callback.run();
                }
            }
        }, player);
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.stopChoosing();
        player.hideView();
        // pick random if one is not chosen
        LGPlayer choosen = null;
        while(choosen == null || choosen == player)
            choosen = getGame().getAlive().get(random.nextInt(getGame().getAlive().size()));
        player.sendRoleFormat(this, "model.info.message", choosen.getName());
        player.sendActionBarRoleFormat(this, "model.info.actionbar", choosen.getName());
        player.getCache().set("enfant_svg", choosen);
        choosen.getCache().set("enfant_svg_d", player);
        getPlayers().remove(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKilled(LGPlayerKilledEvent e) {
        if(e.getGame() == getGame())
            if(e.getKilled().getCache().has("enfant_svg_d")) {
                LGPlayer enfant = e.getKilled().getCache().remove("enfant_svg_d");
                if(!enfant.isDead() && enfant.getCache().remove("enfant_svg") == e.getKilled()) {
                    enfant.sendRoleFormat(this, "model.death", e.getKilled().getName());
                    REnfantSauvageLG lgEnfantSvg = null;
                    for(Role role : getGame().getRoles())
                        if(role instanceof REnfantSauvageLG)
                            lgEnfantSvg = (REnfantSauvageLG) role;

                    if(lgEnfantSvg == null)
                        getGame().getRoles().add(lgEnfantSvg = new REnfantSauvageLG(getGame(), this.getMaxPlayers()));

                    lgEnfantSvg.join(enfant, false);
                }
            }
    }

}
