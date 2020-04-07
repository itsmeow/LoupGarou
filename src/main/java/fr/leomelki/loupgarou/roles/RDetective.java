package fr.leomelki.loupgarou.roles;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;

public class RDetective extends Role {
    public RDetective(LGGame game) {
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
                if(choosen != null) {
                    if(choosen == player) {
                        player.sendRoleFormat(RDetective.this, "choose.self");
                        return;
                    }
                    if(player.getCache().has("detective_first")) {
                        LGPlayer first = player.getCache().remove("detective_first");
                        if(first == choosen) {
                            player.sendRoleFormat(RDetective.this, "choose.sameasprevious", first.getName());
                        } else {
                            if((first.getRoleType() == RoleType.NEUTRAL || choosen.getRoleType() == RoleType.NEUTRAL) ? first.getRole().getClass() == choosen.getRole().getClass() : first.getRoleType() == choosen.getRoleType())
                                player.sendRoleFormat(RDetective.this, "choose.sameteam", first.getName(), choosen.getName());
                            else
                                player.sendRoleFormat(RDetective.this, "choose.differentteam", first.getName(), choosen.getName());

                            player.stopChoosing();
                            player.hideView();
                            callback.run();
                        }
                    } else {
                        player.getCache().set("detective_first", choosen);
                        player.sendRoleFormat(RDetective.this, "choose.initial", choosen.getName());
                    }
                }
            }
        });
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.getCache().remove("detective_first");
        player.stopChoosing();
        player.hideView();
        // player.sendTitle("§cVous n'avez mis personne en couple", "§4Vous avez mis
        // trop de temps à vous décider...", 80);
        // player.sendMessage("§9Tu n'as pas créé de couple.");
    }

}
