package fr.leomelki.loupgarou.roles;

import fr.leomelki.loupgarou.classes.LGGame;

public class RVillageois extends Role {
    public RVillageois(LGGame game) {
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
}
