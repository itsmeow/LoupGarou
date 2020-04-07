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
                    // player.sendTitle("§6Vous avez regardé un rôle",
                    // "§e§l"+choosen.getName()+"§6§l est §e§l"+choosen.getRole().getName(), 5*20);
                    player.sendActionBarMessage("§e§l" + choosen.getName() + "§6 est §e§l" + choosen.getRole().getName());
                    player.sendMessage("§6Tu découvres que §7§l" + choosen.getName() + "§6 est " + choosen.getRole().getName() + "§6.");
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
        // player.sendTitle("§cVous n'avez regardé aucun rôle", "§4Vous avez mis trop de
        // temps à vous décider...", 80);
        // player.sendMessage("§cVous n'avez pas utilisé votre pouvoir cette nuit.");
    }
}
