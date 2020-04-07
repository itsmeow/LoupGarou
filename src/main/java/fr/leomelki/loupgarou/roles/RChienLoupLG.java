package fr.leomelki.loupgarou.roles;

import org.bukkit.potion.PotionEffectType;

import fr.leomelki.loupgarou.classes.LGCustomItems;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.localization.Translate;

public class RChienLoupLG extends Role {
    public RChienLoupLG(LGGame game) {
        super(game);
    }

    @Override
    public String getName(LGPlayer player) {
        for(LGPlayer lgp : getPlayers())
            if(lgp.getPlayer() != null && lgp.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY))
                return roleFormat(player, "name.lg");
        return (getPlayers().size() > 0 ? super.getName(player) : roleFormat(player, "name.lg"));
    }

    @Override
    public RoleType getType() {
        return RoleType.LOUP_GAROU;
    }

    @Override
    public RoleWinType getWinType() {
        return RoleWinType.LOUP_GAROU;
    }

    @Override
    public int getTimeout() {
        return -1;
    }

    @Override
    public void join(LGPlayer player, boolean sendMessage) {
        super.join(player, sendMessage);
        player.setRole(this);
        LGCustomItems.updateItem(player);
        for(Role role : getGame().getRoles())
            if(role instanceof RLoupGarou) {
                role.join(player, false);
                for(LGPlayer lgp : role.getPlayers())
                    if(lgp != player)
                        lgp.sendRoleFormat(this, "join.lg", player.getName());
            }
    }

    // Override to use different ID
    @Override
    public String roleFormat(LGPlayer player, String key) {
        return Translate.get(player, "role.chienloup." + key);
    }

    @Override
    public String roleFormat(LGPlayer player, String key, Object... args) {
        return Translate.get(player, "role.chienloup." + key, args);
    }

}
