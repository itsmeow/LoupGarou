package fr.leomelki.loupgarou.classes;

import fr.leomelki.loupgarou.localization.Translate;

public enum LGWinType {
    VILLAGEOIS,
    LOUPGAROU,
    LOUPGAROUBLANC,
    COUPLE,
    ANGE,
    EQUAL,
    SOLO, // bug si ça s'affiche
    ASSASSIN,
    PYROMANE,
    VAMPIRE,
    NONE;

    public String getMessage(LGPlayer player) {
        return Translate.get(player, "wintype." + this.name().toLowerCase());
    }
}
