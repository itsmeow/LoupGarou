package fr.leomelki.loupgarou.roles;

import org.bukkit.event.EventHandler;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;

public class RGrandMechantLoup extends Role {

    public RGrandMechantLoup(LGGame game, int amount) {
        super(game, amount);
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
        return 15;
    }

    @Override
    public boolean hasPlayersLeft() {
        return super.hasPlayersLeft() && !lgDied;
    }

    boolean lgDied;
    Runnable callback;

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        this.callback = callback;

        player.showView();
        player.choose(new LGChooseCallback() {
            @Override
            public void callback(LGPlayer choosen) {
                if(choosen != null && choosen != player) {
                    player.sendActionBarRoleFormat(RGrandMechantLoup.this, "choose.actionbar", choosen.getName());
                    player.sendRoleFormat(RGrandMechantLoup.this, "choose.message", choosen.getName());
                    getGame().kill(choosen, getGame().getDeaths().containsKey(Reason.LOUP_GAROU) ? Reason.GM_LOUP_GAROU : Reason.LOUP_GAROU);
                    player.stopChoosing();
                    player.hideView();
                    callback.run();
                }
            }
        });
    }

    @EventHandler
    public void onPlayerDie(LGPlayerKilledEvent e) {// Quand un Loup-Garou meurt, les grands méchants loups ne peuvent plus jouer.
        if(e.getGame() == getGame())
            if(e.getKilled().getRoleType() == RoleType.LOUP_GAROU)
                lgDied = true;
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.stopChoosing();
        player.hideView();
        player.sendRoleFormat(this, "timeout");
    }

    @Override
    public void join(LGPlayer player, boolean sendMessage) {
        super.join(player, sendMessage);
        for(Role role : getGame().getRoles())
            if(role instanceof RLoupGarou)
                role.join(player, false);
    }

}
