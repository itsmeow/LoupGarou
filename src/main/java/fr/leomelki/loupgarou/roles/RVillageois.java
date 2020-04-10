package fr.leomelki.loupgarou.roles;

import fr.leomelki.loupgarou.classes.LGGame;

public class RVillageois extends Role {
    public RVillageois(LGGame game, int amount) {
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
}
