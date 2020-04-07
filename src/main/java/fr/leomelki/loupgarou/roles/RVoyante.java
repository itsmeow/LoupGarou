package fr.leomelki.loupgarou.roles;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;

public class RVoyante extends Role {
    public RVoyante(LGGame game) {
        super(game);
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
                    player.sendActionBarRoleFormat(RVoyante.this, "reveal.actionbar", choosen.getName(), choosen.getRole().getName(player));
                    player.sendRoleFormat(RVoyante.this, "reveal.message", choosen.getName(), choosen.getRole().getName(player));
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
