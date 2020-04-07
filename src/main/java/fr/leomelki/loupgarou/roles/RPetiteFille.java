package fr.leomelki.loupgarou.roles;

import org.bukkit.event.EventHandler;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGRoleTurnEndEvent;

public class RPetiteFille extends Role {
    public RPetiteFille(LGGame game) {
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
        return -1;
    }

    @EventHandler
    public void onChangeRole(LGRoleTurnEndEvent e) {
        if(e.getGame() == getGame()) {
            if(e.getNewRole() instanceof RLoupGarou) {
                for(Role role : getGame().getRoles()) {
                    if(role instanceof RLoupGarou) {
                        RLoupGarou lgRole = (RLoupGarou) role;
                        for(LGPlayer player : getPlayers())
                            if(!player.getCache().getBoolean("infected"))
                                player.joinChat(lgRole.getChat(), (sender, message) -> {
                                    return "§c" + roleFormat(player, "spynames").split(",")[lgRole.getPlayers().indexOf(sender)] + " §6» §f" + message;
                                }, true);
                        break;
                    }
                }
            }
            if(e.getPreviousRole() instanceof RLoupGarou) {
                for(LGPlayer player : getPlayers()) {
                    if(!player.getCache().getBoolean("infected")) {
                        player.leaveChat();
                    }
                }
            }
        }
    }
}
