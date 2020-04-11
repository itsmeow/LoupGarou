package fr.leomelki.loupgarou.roles;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;

public class RPronostiqueur extends Role {

    public RPronostiqueur(LGGame game, int amount) {
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
                    String status = choosen.getRoleWinType() == RoleWinType.VILLAGE || choosen.getRoleWinType() == RoleWinType.NONE ? "good" : "bad";
                    player.sendActionBarRoleFormat(RPronostiqueur.this, "choose.actionbar." + status, choosen.getName());
                    player.sendRoleFormat(RPronostiqueur.this, "choose.message." + status, choosen.getName());
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
