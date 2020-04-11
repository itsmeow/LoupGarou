package fr.leomelki.loupgarou.roles;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;

public class RChasseurDeVampire extends Role {

    public RChasseurDeVampire(LGGame game, int amount) {
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
    public boolean hasPlayersLeft() {
        for(LGPlayer lgp : getGame().getAlive())
            if(lgp.getRoleType() == RoleType.VAMPIRE)
                return super.hasPlayersLeft();
        return false;
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        player.showView();

        player.choose(new LGChooseCallback() {
            @Override
            public void callback(LGPlayer choosen) {
                if(choosen != null && choosen != player) {
                    if(choosen.getCache().getBoolean("vampire") || choosen.getRole() instanceof RVampire) {
                        getGame().kill(choosen, Reason.CHASSEUR_DE_VAMPIRE);
                        player.sendRoleFormat(RChasseurDeVampire.this, "choose.vampire.message", choosen.getName());
                        player.sendActionBarRoleFormat(RChasseurDeVampire.this, "choose.vampire.actionbar", choosen.getName());
                    } else {
                        player.sendRoleFormat(RChasseurDeVampire.this, "choose.nonvampire.message", choosen.getName());
                        player.sendActionBarRoleFormat(RChasseurDeVampire.this, "choose.nonvampire.actionbar", choosen.getName());
                    }

                    player.stopChoosing();
                    player.hideView();
                    callback.run();
                }
            }
        });
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.stopChoosing();
        player.hideView();
    }
}
