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
    NONE;

    public String getMessage() {
        return Translate.get("wintype." + this.name().toLowerCase());
    }
}
