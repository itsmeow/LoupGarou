package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;

import org.bukkit.event.EventHandler;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.events.LGDayEndEvent;
import fr.leomelki.loupgarou.events.LGEndCheckEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGPlayerGotKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGVoteEvent;

public class RAnge extends Role {
    public RAnge(LGGame game, int amount) {
        super(game, amount);
    }

    @Override
    public RoleType getType() {
        return RoleType.NEUTRAL;
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
    public void onVoteStart(LGVoteEvent e) {
        if(e.getGame() == getGame()) {
            night = getGame().getNight();
            vote = true;
            for(LGPlayer lgp : getPlayers())
                if(!lgp.isDead() && lgp.isRoleActive())
                    lgp.sendRoleFormat(this, "reminder");
        }
    }

    boolean vote;

    @EventHandler
    public void onDayEnd(LGDayEndEvent e) {
        if(e.getGame() == getGame()) {
            if(getPlayers().size() > 0 && getGame().getNight() == night + 1 && vote) {
                Role villageois = null;
                for(Role role : getGame().getRoles()) {
                    if(role instanceof RVillageois)
                        villageois = role;
                }

                if(villageois == null)
                    getGame().getRoles().add(villageois = new RVillageois(getGame(), 1));

                for(LGPlayer lgp : getPlayers()) {
                    if(lgp.isRoleActive()) {
                        lgp.sendRoleFormat(this, "failed");
                    }
                    lgp.setRole(villageois);
                    villageois.join(lgp);
                }

                getPlayers().clear();
                getGame().updateRoleScoreboard();
            }
            vote = false;
        }
    }

    ArrayList<LGPlayer> winners = new ArrayList<LGPlayer>();
    int night = 1;

    @EventHandler
    public void onDeath(LGPlayerGotKilledEvent e) {
        if(e.getGame() == getGame())
            if(e.getReason() == Reason.VOTE && e.getKilled().getRole() == this && getGame().getNight() == night && e.getKilled().isRoleActive())
                winners.add(e.getKilled());
    }

    @EventHandler
    public void onWinCheck(LGEndCheckEvent e) {
        if(e.getGame() == getGame())
            if(winners.size() > 0)
                e.setWinType(winners.size() == 1 && winners.get(0).getCache().has("inlove") ? LGWinType.COUPLE : LGWinType.ANGE);
    }

    @EventHandler
    public void onWin(LGGameEndEvent e) {
        if(e.getGame() == getGame())
            if(e.getWinType() == LGWinType.ANGE)
                e.getWinners().addAll(winners);
    }
}
