package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerScoreboardTeam;
import fr.leomelki.loupgarou.classes.LGCustomSkin;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGVote;
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.classes.chat.LGChat;
import fr.leomelki.loupgarou.events.LGDayEndEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGNightEndEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGSkinLoadEvent;
import fr.leomelki.loupgarou.events.LGUpdatePrefixEvent;
import lombok.Getter;

public class RLoupGarou extends Role {

    public RLoupGarou(LGGame game, int amount) {
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
        return 30;
    }

    @Getter
    private LGChat chat = new LGChat((sender, message) -> {
        return "§c" + sender.getName() + " §6» §f" + message;
    });

    boolean showSkins = false;
    LGVote vote;

    @Override
    public void join(LGPlayer player, boolean sendMessage) {
        super.join(player, sendMessage);
        // On peut créer des cheats grâce à ça (qui permettent de savoir qui est lg/inf)
        for(LGPlayer p : getPlayers())
            p.updatePrefix();
    }

    public void onNightTurn(Runnable callback) {
        vote = new LGVote(getTimeout(), getTimeout() / 3, getGame(), false, (player, secondsLeft) -> {
            return !getPlayers().contains(player) ? roleFormat(player, "nightturn.broadcast", getFriendlyName(player), secondsLeft) : player.getCache().has("vote") ? roleFormat(player, "nightturn.voteagainst", player.getCache().<LGPlayer>get("vote").getName()) : roleFormat(player, "nightturn.voting", secondsLeft);
        });
        for(LGPlayer lgp : getGame().getAlive())
            if(lgp.getRoleType() == RoleType.LOUP_GAROU)
                lgp.showView();
        for(LGPlayer player : getPlayers()) {
            player.sendMessage("§6" + getTask(player));
            // player.sendTitle("§6C'est à vous de jouer", "§a"+getTask(), 100);
            player.joinChat(chat);
        }
        vote.start(getPlayers(), getPlayers(), () -> {
            onNightTurnEnd();
            callback.run();
        });
    }

    private void onNightTurnEnd() {
        for(LGPlayer lgp : getGame().getAlive())
            if(lgp.getRoleType() == RoleType.LOUP_GAROU)
                lgp.hideView();
        for(LGPlayer player : getPlayers()) {
            player.leaveChat();
        }

        LGPlayer choosen = vote.getChoosen();
        if(choosen == null) {
            if(vote.getVotes().size() > 0) {
                int max = 0;
                boolean equal = false;
                for(Entry<LGPlayer, List<LGPlayer>> entry : vote.getVotes().entrySet())
                    if(entry.getValue().size() > max) {
                        equal = false;
                        max = entry.getValue().size();
                        choosen = entry.getKey();
                    } else if(entry.getValue().size() == max)
                        equal = true;
                if(equal) {
                    choosen = null;
                    ArrayList<LGPlayer> choosable = new ArrayList<LGPlayer>();
                    for(Entry<LGPlayer, List<LGPlayer>> entry : vote.getVotes().entrySet())
                        if(entry.getValue().size() == max && entry.getKey().getRoleType() != RoleType.LOUP_GAROU)
                            choosable.add(entry.getKey());
                    if(choosable.size() > 0)
                        choosen = choosable.get(getGame().getRandom().nextInt(choosable.size()));
                }
            }
        }
        if(choosen != null) {
            getGame().kill(choosen, Reason.LOUP_GAROU);
            for(LGPlayer player : getPlayers())
                player.sendRoleFormat(this, "nightend.kill", choosen.getName());
        } else
            for(LGPlayer player : getPlayers())
                player.sendRoleFormat(this, "nightend.nokill");
    }

    @EventHandler
    public void onGameJoin(LGGameEndEvent e) {
        if(e.getGame() == getGame()) {
            WrapperPlayServerScoreboardTeam teamDelete = new WrapperPlayServerScoreboardTeam();
            teamDelete.setMode(1);
            teamDelete.setName("loup_garou_list");

            for(LGPlayer lgp : getGame().getInGame())
                teamDelete.sendPacket(lgp.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSkinChange(LGSkinLoadEvent e) {
        if(e.getGame() == getGame())
            if(getPlayers().contains(e.getPlayer()) && getPlayers().contains(e.getTo()) && showSkins) {
                e.getProfile().getProperties().removeAll("textures");
                e.getProfile().getProperties().put("textures", LGCustomSkin.WEREWOLF.getProperty());
            }
    }

    @EventHandler
    public void onGameEnd(LGGameEndEvent e) {
        if(e.getGame() == getGame() && e.getWinType() == LGWinType.LOUPGAROU)
            for(LGPlayer lgp : getGame().getInGame())
                if(lgp.getRoleWinType() == RoleWinType.LOUP_GAROU)// Changed to wintype
                    e.getWinners().add(lgp);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUpdatePrefix(LGUpdatePrefixEvent e) {
        if(e.getGame() == getGame())
            if(getPlayers().contains(e.getTo()) && getPlayers().contains(e.getPlayer()))
                e.setPrefix(e.getPrefix() + "§c");
    }

    @EventHandler
    public void onDay(LGNightEndEvent e) {
        if(e.getGame() == getGame()) {
            showSkins = false;
            for(LGPlayer player : getPlayers())
                player.updateOwnSkin();
        }
    }

    @EventHandler
    public void onNight(LGDayEndEvent e) {
        if(e.getGame() == getGame()) {
            showSkins = true;
            for(LGPlayer player : getPlayers())
                player.updateOwnSkin();
        }
    }

}
