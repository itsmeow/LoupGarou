package fr.leomelki.loupgarou.roles;

import java.util.Arrays;
import java.util.List;

import org.bukkit.event.EventHandler;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGNightPlayerPreKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGPreDayStartEvent;
import fr.leomelki.loupgarou.events.LGVampiredEvent;

public class RGarde extends Role {
    public RGarde(LGGame game, int amount) {
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
        return 15;
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        player.showView();

        player.choose(new LGChooseCallback() {
            @Override
            public void callback(LGPlayer choosen) {
                if(choosen != null) {
                    LGPlayer lastProtected = player.getCache().get("garde_lastProtected");
                    if(choosen == lastProtected) {
                        if(lastProtected == player)
                            player.sendRoleFormat(RGarde.this, "protect.repeat.self");
                        else
                            player.sendRoleFormat(RGarde.this, "protect.repeat.other", lastProtected.getName());
                    } else {
                        if(choosen == player) {
                            player.sendRoleFormat(RGarde.this, "protect.self.message");
                            player.sendActionBarRoleFormat(RGarde.this, "protect.self.actionbar");
                        } else {
                            player.sendRoleFormat(RGarde.this, "protect.other.message", choosen.getName());
                            player.sendActionBarRoleFormat(RGarde.this, "protect.other.actionbar", choosen.getName());
                        }
                        choosen.getCache().set("garde_protected", true);
                        player.getCache().set("garde_lastProtected", choosen);
                        player.stopChoosing();
                        player.hideView();
                        callback.run();
                    }
                }
            }
        });
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.getCache().remove("garde_lastProtected");
        player.stopChoosing();
        player.hideView();
        // player.sendTitle("§cVous n'avez protégé personne.", "§4Vous avez mis trop de
        // temps à vous décider...", 80);
        // player.sendMessage("§cVous n'avez protégé personne cette nuit.");
    }

    private static List<Reason> reasonsProtected = Arrays.asList(Reason.LOUP_GAROU, Reason.LOUP_BLANC, Reason.GM_LOUP_GAROU, Reason.ASSASSIN);

    @EventHandler
    public void onPlayerKill(LGNightPlayerPreKilledEvent e) {
        if(e.getGame() == getGame() && reasonsProtected.contains(e.getReason()) && e.getKilled().getCache().getBoolean("garde_protected")) {
            e.getKilled().getCache().remove("garde_protected");
            e.setReason(Reason.DONT_DIE);
        }
    }

    @EventHandler
    public void onVampired(LGVampiredEvent e) {
        if(e.getGame() == getGame() && e.getPlayer().getCache().getBoolean("garde_protected"))
            e.setProtect(true);
    }

    @EventHandler
    public void onDayStart(LGPreDayStartEvent e) {
        if(e.getGame() == getGame())
            for(LGPlayer lgp : getGame().getInGame())
                lgp.getCache().remove("garde_protected");
    }
}
