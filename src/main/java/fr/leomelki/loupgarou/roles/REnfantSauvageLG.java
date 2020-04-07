package fr.leomelki.loupgarou.roles;

import java.util.Comparator;

import org.bukkit.potion.PotionEffectType;

import fr.leomelki.loupgarou.classes.LGCustomItems;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.localization.Translate;

public class REnfantSauvageLG extends Role {
    public REnfantSauvageLG(LGGame game) {
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
        RLoupGarou lgRole = null;
        for(Role role : getGame().getRoles())
            if(role instanceof RLoupGarou)
                lgRole = (RLoupGarou) role;

        if(lgRole == null) {
            getGame().getRoles().add(lgRole = new RLoupGarou(getGame()));

            getGame().getRoles().sort(new Comparator<Role>() {
                @Override
                public int compare(Role role1, Role role2) {
                    return role1.getTurnOrder() - role2.getTurnOrder();
                }
            });
        }

        lgRole.join(player, false);
        for(LGPlayer lgp : lgRole.getPlayers())
            if(lgp != player)
                lgp.sendRoleFormat(this, "join.lg", player.getName());
    }

    // Override to use different ID
    @Override
    public String roleFormat(LGPlayer player, String key) {
        return Translate.get(player, "role.enfantsauvage." + key);
    }

    @Override
    public String roleFormat(LGPlayer player, String key, Object... args) {
        return Translate.get(player, "role.enfantsauvage." + key, args);
    }
}
