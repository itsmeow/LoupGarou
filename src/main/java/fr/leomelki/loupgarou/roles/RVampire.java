package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import fr.leomelki.loupgarou.classes.LGCustomItems;
import fr.leomelki.loupgarou.classes.LGCustomItems.LGCustomItemsConstraints;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGVote;
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.classes.chat.LGChat;
import fr.leomelki.loupgarou.events.LGCustomItemChangeEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGNightEndEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGUpdatePrefixEvent;
import fr.leomelki.loupgarou.events.LGVampiredEvent;
import fr.leomelki.loupgarou.localization.Translate;
import lombok.Getter;

public class RVampire extends Role {

    public RVampire(LGGame game, int amount) {
        super(game, amount);
    }

    @Override
    public RoleType getType() {
        return RoleType.VAMPIRE;
    }

    @Override
    public RoleWinType getWinType() {
        return RoleWinType.VAMPIRE;
    }

    @Override
    public int getTimeout() {
        return 30;
    }

    @Override
    public boolean hasPlayersLeft() {
        return nextCanInfect < getGame().getNight() && super.hasPlayersLeft();
    }

    @Getter
    private LGChat chat = new LGChat((sender, message) -> {
        return "§5" + sender.getName() + " §6» §f" + message;
    });
    int nextCanInfect = 0;
    LGVote vote;

    @Override
    public void join(LGPlayer player, boolean sendMessage) {
        super.join(player, sendMessage);
        for(LGPlayer p : getPlayers())
            p.updatePrefix();
    }

    public void onNightTurn(Runnable callback) {
        vote = new LGVote(getTimeout(), getTimeout() / 3, getGame(), false, (player, secondsLeft) -> {
            if(!getPlayers().contains(player)) {
                return Translate.get(player, "role.generic.othersturn", getFriendlyName(player), secondsLeft);
            } else if(player.getCache().has("vote")) {
                return roleFormat(player, "nightturn.vote", player.getCache().<LGPlayer>get("vote").getName());
            } else {
                return Translate.get(player, "role.loupgarou.nightturn.voting", secondsLeft);
            }
        });
        for(LGPlayer lgp : getGame().getAlive())
            if(lgp.getRoleType() == RoleType.VAMPIRE)
                lgp.showView();
        for(LGPlayer player : getPlayers()) {
            player.sendMessage("§6" + getTask(player));
            player.joinChat(chat);
        }
        vote.start(getPlayers(), getPlayers(), () -> {
            onNightTurnEnd();
            callback.run();
        }, getPlayers());
    }

    private void onNightTurnEnd() {
        for(LGPlayer lgp : getGame().getAlive())
            if(lgp.getRoleType() == RoleType.VAMPIRE)
                lgp.hideView();
        for(LGPlayer player : getPlayers())
            player.leaveChat();

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
                        if(entry.getValue().size() == max && entry.getKey().getRoleType() != RoleType.VAMPIRE)
                            choosable.add(entry.getKey());
                    if(choosable.size() > 0)
                        choosen = choosable.get(getGame().getRandom().nextInt(choosable.size()));
                }
            }
        }
        if(choosen != null) {
            if(choosen.getRoleType() == RoleType.LOUP_GAROU || choosen.getRoleType() == RoleType.VAMPIRE) {
                for(LGPlayer player : getPlayers())
                    player.sendFormat("role.generic.targetimmune");
                return;
            } else if(choosen.getRole() instanceof RChasseurDeVampire) {
                for(LGPlayer player : getPlayers())
                    player.sendFormat("role.generic.targetimmune");
                getGame().kill(getPlayers().get(getPlayers().size() - 1), Reason.CHASSEUR_DE_VAMPIRE);
                return;
            }

            LGVampiredEvent event = new LGVampiredEvent(getGame(), choosen);
            Bukkit.getPluginManager().callEvent(event);
            if(event.isImmuned()) {
                for(LGPlayer player : getPlayers())
                    player.sendFormat("role.generic.targetimmune");
                return;
            } else if(event.isProtect()) {
                for(LGPlayer player : getPlayers())
                    player.sendFormat("role.generic.targetprotected");
                return;
            }
            for(LGPlayer player : getPlayers())
                player.sendRoleFormat(this, "transform.broadcast", choosen.getName());
            choosen.sendRoleFormat(this, "transform.inform1");
            choosen.sendRoleFormat(this, "transform.inform2");
            choosen.getCache().set("vampire", true);
            choosen.getCache().set("just_vampire", true);
            nextCanInfect = getGame().getNight() + 1;
            join(choosen, false);
            LGCustomItems.updateItem(choosen);
        } else
            for(LGPlayer player : getPlayers())
                player.sendRoleFormat(this, "notransform.broadcast");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDayStart(LGNightEndEvent e) {
        if(e.getGame() == getGame())
            for(LGPlayer player : getGame().getAlive()) {
                if(player.getCache().getBoolean("just_vampire")) {
                    player.getCache().remove("just_vampire");
                    for(LGPlayer lgp : getGame().getInGame()) {
                        if(lgp.getRoleType() == RoleType.VAMPIRE)
                            lgp.sendRoleFormat(this, "transform.broadcast", player.getName());
                        else
                            lgp.sendRoleFormat(this, "transform.broadcast.others");
                    }

                    if(getGame().checkEndGame())
                        e.setCancelled(true);
                }
            }
    }

    @EventHandler
    public void onGameEnd(LGGameEndEvent e) {
        if(e.getGame() == getGame() && e.getWinType() == LGWinType.VAMPIRE)
            for(LGPlayer lgp : getGame().getInGame())
                if(lgp.getRoleWinType() == RoleWinType.VAMPIRE)// Changed to wintype
                    e.getWinners().add(lgp);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUpdatePrefix(LGUpdatePrefixEvent e) {
        if(e.getGame() == getGame())
            if(getPlayers().contains(e.getTo()) && getPlayers().contains(e.getPlayer()))
                e.setPrefix(e.getPrefix() + "§5");
    }

    @EventHandler
    public void onCustomItemChange(LGCustomItemChangeEvent e) {
        if(e.getGame() == getGame())
            if(e.getPlayer().getCache().getBoolean("vampire"))
                e.getConstraints().add(LGCustomItemsConstraints.VAMPIRE_INFECTE.getName());
    }

}
