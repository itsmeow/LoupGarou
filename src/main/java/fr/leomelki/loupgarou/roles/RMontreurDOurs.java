package fr.leomelki.loupgarou.roles;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGDayStartEvent;

public class RMontreurDOurs extends Role {
    private int lastNight = -1;

    public RMontreurDOurs(LGGame game, int amount) {
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
        return -1;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDay(LGDayStartEvent e) {
        if(e.getGame() == getGame() && getPlayers().size() > 0) {
            if(lastNight == getGame().getNight())
                return;
            lastNight = getGame().getNight();
            List<?> original = MainLg.getInstance().getConfig().getList("spawns");
            for(LGPlayer target : getPlayers()) {
                if(!target.isRoleActive())
                    continue;
                int size = original.size();
                int killedPlace = target.getPlace();

                for(int i = killedPlace + 1;; i++) {
                    if(i == size)
                        i = 0;
                    LGPlayer lgp = getGame().getPlacements().get(i);
                    if(lgp != null && !lgp.isDead()) {
                        if(lgp.getRoleWinType() == RoleWinType.VILLAGE || lgp.getRoleWinType() == RoleWinType.NONE)
                            break;
                        else {
                            getGame().broadcastFunction(lg -> roleFormat(lg, "growl.broadcast", getName(lg)));
                            return;
                        }
                    }
                    if(lgp == target)// Fait un tour complet
                        break;
                }
                for(int i = killedPlace - 1;; i--) {
                    if(i == -1)
                        i = size - 1;
                    LGPlayer lgp = getGame().getPlacements().get(i);
                    if(lgp != null && !lgp.isDead()) {
                        if(lgp.getRoleWinType() == RoleWinType.VILLAGE || lgp.getRoleWinType() == RoleWinType.NONE)
                            break;
                        else {
                            getGame().broadcastFunction(lg -> roleFormat(lg, "growl.broadcast", getName(lg)));
                            return;
                        }
                    }
                    if(lgp == target)// Fait un tour complet
                        break;
                }
            }
        }
    }
}
