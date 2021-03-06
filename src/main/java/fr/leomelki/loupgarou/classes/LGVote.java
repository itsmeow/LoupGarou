package fr.leomelki.loupgarou.classes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.EntityType;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityDestroy;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityLook;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerSpawnEntityLiving;
import fr.leomelki.loupgarou.classes.LGGame.TextGenerator;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGVoteLeaderChange;
import fr.leomelki.loupgarou.localization.Translate;
import fr.leomelki.loupgarou.utils.VariousUtils;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.EntityArmorStand;

public class LGVote {
    @Getter
    LGPlayer choosen;
    private int timeout, initialTimeout, littleTimeout;
    private Runnable callback;
    private final LGGame game;
    @Getter
    private List<LGPlayer> participants, viewers;
    private final TextGenerator generator;
    @Getter
    private final HashMap<LGPlayer, List<LGPlayer>> votes = new HashMap<LGPlayer, List<LGPlayer>>();
    private int votesSize = 0;
    private LGPlayer mayor;
    private ArrayList<LGPlayer> latestTop = new ArrayList<LGPlayer>();
    private ArrayList<LGPlayer> blacklisted = new ArrayList<LGPlayer>();
    private final boolean randomIfEqual;
    @Getter
    private boolean mayorVote;
    private boolean ended;

    public LGVote(int timeout, int littleTimeout, LGGame game, boolean randomIfEqual, TextGenerator generator) {
        this.littleTimeout = littleTimeout;
        this.initialTimeout = timeout;
        this.timeout = timeout;
        this.game = game;
        this.generator = generator;
        this.randomIfEqual = randomIfEqual;
    }

    public void start(List<LGPlayer> participants, List<LGPlayer> viewers, Runnable callback) {
        this.callback = callback;
        this.participants = participants;
        this.viewers = viewers;
        game.wait(timeout, this::end, generator);
        for(LGPlayer player : participants)
            player.choose(getChooseCallback(player));
    }

    public void start(List<LGPlayer> participants, List<LGPlayer> viewers, Runnable callback, ArrayList<LGPlayer> blacklisted) {
        this.callback = callback;
        this.participants = participants;
        this.viewers = viewers;
        game.wait(timeout, this::end, generator);
        for(LGPlayer player : participants)
            player.choose(getChooseCallback(player));
        this.blacklisted = blacklisted;
    }

    public void start(List<LGPlayer> participants, List<LGPlayer> viewers, Runnable callback, LGPlayer mayor) {
        this.callback = callback;
        this.participants = participants;
        this.viewers = viewers;
        this.mayor = mayor;
        game.wait(timeout, this::end, generator);
        for(LGPlayer player : participants)
            player.choose(getChooseCallback(player));
    }

    private void end() {
        ended = true;
        for(LGPlayer lgp : viewers)
            showVoting(lgp, null);
        for(LGPlayer lgp : votes.keySet())
            updateVotes(lgp, true);
        int max = 0;
        boolean equal = false;
        for(Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
            if(entry.getValue().size() > max) {
                equal = false;
                max = entry.getValue().size();
                choosen = entry.getKey();
            } else if(entry.getValue().size() == max)
                equal = true;
        for(LGPlayer player : participants) {
            player.getCache().remove("vote");
            player.stopChoosing();
        }
        if(equal)
            choosen = null;
        if(equal && mayor == null && randomIfEqual) {
            ArrayList<LGPlayer> choosable = new ArrayList<LGPlayer>();
            for(Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
                if(entry.getValue().size() == max)
                    choosable.add(entry.getKey());
            choosen = choosable.get(game.getRandom().nextInt(choosable.size()));
        }

        if(equal && mayor != null && max != 0) {
            for(LGPlayer player : viewers)
                player.sendFormat("voting.tiebreaker.broadcast");
            mayor.sendFormat("voting.tiebreaker.message");

            ArrayList<LGPlayer> choosable = new ArrayList<LGPlayer>();
            for(Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
                if(entry.getValue().size() == max)
                    choosable.add(entry.getKey());

            for(int i = 0; i < choosable.size(); i++) {
                LGPlayer lgp = choosable.get(i);
                showArrow(mayor, lgp, -mayor.getPlayer().getEntityId() - i);
            }

            StringJoiner sj = new StringJoiner(", ");
            for(int i = 0; i < choosable.size() - 1; i++)
                sj.add(choosable.get(0).getName());
            ArrayList<LGPlayer> blackListed = new ArrayList<LGPlayer>();
            for(LGPlayer player : participants)
                if(!choosable.contains(player))
                    blackListed.add(player);
                else {
                    VariousUtils.setWarning(player.getPlayer(), true);
                }
            mayorVote = true;
            game.wait(30, () -> {
                for(LGPlayer player : participants)
                    if(choosable.contains(player))
                        VariousUtils.setWarning(player.getPlayer(), false);

                for(int i = 0; i < choosable.size(); i++) {
                    showArrow(mayor, null, -mayor.getPlayer().getEntityId() - i);
                }
                // Choix au hasard d'un joueur si personne n'a été désigné
                choosen = choosable.get(game.getRandom().nextInt(choosable.size()));
                callback.run();
            }, (player, secondsLeft) -> {
                timeout = secondsLeft;
                return mayor == player ? Translate.get(player, "voting.tiebreaker.choose.mayor", secondsLeft) : Translate.get(player, "voting.tiebreaker.choose.others", secondsLeft);
            });
            mayor.choose(new LGChooseCallback() {

                @Override
                public void callback(LGPlayer choosen) {
                    if(choosen != null) {
                        if(blackListed.contains(choosen)) {
                            mayor.sendFormat("voting.tiebreaker.choose.blacklisted");
                        } else {
                            for(LGPlayer player : participants)
                                if(choosable.contains(player))
                                    VariousUtils.setWarning(player.getPlayer(), false);

                            for(int i = 0; i < choosable.size(); i++) {
                                showArrow(mayor, null, -mayor.getPlayer().getEntityId() - i);
                            }
                            game.cancelWait();
                            LGVote.this.choosen = choosen;
                            callback.run();
                        }
                    }
                }
            });
        } else {
            game.cancelWait();
            callback.run();
        }

    }

    public LGChooseCallback getChooseCallback(LGPlayer who) {
        return new LGChooseCallback() {

            @Override
            public void callback(LGPlayer choosen) {
                if(choosen != null)
                    vote(who, choosen);
            }
        };
    }

    public void vote(LGPlayer voter, LGPlayer voted) {
        voteNamed(lg -> voter.getName(), voter, voted);
    }

    public void voteNamed(Function<LGPlayer, String> voterName, LGPlayer voter, LGPlayer voted) {
        if(blacklisted.contains(voted)) {
            voter.sendFormat("voting.blacklisted", voted.getName());
            return;
        }
        if(voted == voter.getCache().get("vote"))
            voted = null;

        if(voted != null && voter.getPlayer() != null)
            votesSize++;
        if(voter.getCache().has("vote"))
            votesSize--;

        if(votesSize == participants.size() && game.getWaitTicks() > littleTimeout * 20) {
            votesSize = 999;
            game.wait(littleTimeout, initialTimeout, this::end, generator);
        }
        boolean changeVote = false;
        if(voter.getCache().has("vote")) {// On enl�ve l'ancien vote
            LGPlayer devoted = voter.getCache().get("vote");
            if(votes.containsKey(devoted)) {
                List<LGPlayer> voters = votes.get(devoted);
                if(voters != null) {
                    voters.remove(voter);
                    if(voters.size() == 0)
                        votes.remove(devoted);
                }
            }
            voter.getCache().remove("vote");
            updateVotes(devoted);
            changeVote = true;
        }

        if(voted != null) {// Si il vient de voter, on ajoute le nouveau vote
            if(votes.containsKey(voted))
                votes.get(voted).add(voter);
            else
                votes.put(voted, new ArrayList<LGPlayer>(Arrays.asList(voter)));
            voter.getCache().set("vote", voted);
            updateVotes(voted);
        }
        if(voter.getPlayer() != null) {
            showVoting(voter, voted);
        }
        sendVoteMessages(voterName, voter, voted, changeVote);
    }

    private void sendVoteMessages(Function<LGPlayer, String> broadcastVoterName, LGPlayer voter, LGPlayer voted, boolean changeVote) {
        if(voter.getPlayer() != null) {
            String key = "";
            if(voted != null) {
                if(!blacklisted.contains(voted)) {
                    if(changeVote) {
                        key = "voting.broadcast.changevote";
                        voter.sendFormat("voting.message.changevote", voted.getName());
                    } else {
                        key = "voting.broadcast.vote";
                        voter.sendFormat("voting.message.vote", voted.getName());
                    }
                    for(LGPlayer player : viewers) {
                        if(player != voter) {
                            player.sendFormat(key, broadcastVoterName.apply(player), voted.getName());
                        }
                    }
                }
            } else {
                key = "voting.broadcast.cancel";
                voter.sendFormat("voting.message.cancel");
                for(LGPlayer player : viewers) {
                    if(player != voter) {
                        player.sendFormat(key, broadcastVoterName.apply(player));
                    }
                }
            }
        }
    }

    public List<LGPlayer> getVotes(LGPlayer voted) {
        return votes.containsKey(voted) ? votes.get(voted) : new ArrayList<LGPlayer>(0);
    }

    private void updateVotes(LGPlayer voted) {
        updateVotes(voted, false);
    }

    private static EntityArmorStand armorStand = new EntityArmorStand(((CraftWorld)Bukkit.getWorlds().get(0)).getHandle(), 0, 0, 0);
    static {
        armorStand.setCustomNameVisible(true);
        armorStand.setInvisible(true);
        armorStand.setGravity(false);
        armorStand.setCustomName(ChatColor.translateAlternateColorCodes('&', "&a&l✿"));
    }
    
    private static EntityArmorStand armorStand2 = new EntityArmorStand(((CraftWorld)Bukkit.getWorlds().get(0)).getHandle(), 0, 0, 0);
    static {
        armorStand2.setCustomNameVisible(true);
        armorStand2.setInvisible(true);
        armorStand2.setGravity(false);
    }

    private void updateVotes(LGPlayer voted, boolean kill) {
        int entityId = Integer.MIN_VALUE + voted.getPlayer().getEntityId();
        WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
        destroy.setEntityIds(new int[] { entityId });
        for(LGPlayer lgp : viewers)
            destroy.sendPacket(lgp.getPlayer());

        if(!kill) {
            int max = 0;
            for(Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
                if(entry.getValue().size() > max)
                    max = entry.getValue().size();
            ArrayList<LGPlayer> last = latestTop;
            latestTop = new ArrayList<LGPlayer>();
            for(Entry<LGPlayer, List<LGPlayer>> entry : votes.entrySet())
                if(entry.getValue().size() == max)
                    latestTop.add(entry.getKey());
            Bukkit.getPluginManager().callEvent(new LGVoteLeaderChange(game, this, last, latestTop));
        }

        if(votes.containsKey(voted) && !kill) {
            Location loc = voted.getPlayer().getLocation();

            WrapperPlayServerSpawnEntityLiving spawn = new WrapperPlayServerSpawnEntityLiving();
            spawn.setEntityID(entityId);
            spawn.setType(EntityType.ARMOR_STAND);
            spawn.setX(loc.getX());
            spawn.setY(loc.getY() + 0.3);
            spawn.setZ(loc.getZ());

            int votesNbr = votes.get(voted).size();

            for(LGPlayer lgp : viewers) {
                armorStand2.setCustomName(Translate.getColor(lgp, "voting.display.head", votesNbr));
                spawn.setMetadata(WrappedDataWatcher.getEntityWatcher(armorStand.getBukkitEntity()));
                spawn.sendPacket(lgp.getPlayer());
            }
        }
    }

    private static void showVoting(LGPlayer to, LGPlayer ofWho) {
        int entityId = -to.getPlayer().getEntityId();
        WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
        destroy.setEntityIds(new int[] { entityId });
        destroy.sendPacket(to.getPlayer());
        if(ofWho != null) {
            WrapperPlayServerSpawnEntityLiving spawn = new WrapperPlayServerSpawnEntityLiving();
            spawn.setEntityID(entityId);
            spawn.setType(EntityType.ARMOR_STAND);
            Location loc = ofWho.getPlayer().getLocation();
            spawn.setX(loc.getX());
            spawn.setY(loc.getY() + 1.3);
            spawn.setZ(loc.getZ());
            spawn.setHeadPitch(0);
            Location toLoc = to.getPlayer().getLocation();
            double diffX = loc.getX() - toLoc.getX(),
            diffZ = loc.getZ() - toLoc.getZ();
            float yaw = 180 - ((float) Math.toDegrees(Math.atan2(diffX, diffZ)));

            spawn.setYaw(yaw);
            spawn.sendPacket(to.getPlayer());
            spawn.setMetadata(WrappedDataWatcher.getEntityWatcher(armorStand.getBukkitEntity()));

            WrapperPlayServerEntityLook look = new WrapperPlayServerEntityLook();
            look.setEntityID(entityId);
            look.setPitch(0);
            look.setYaw(yaw);
            look.sendPacket(to.getPlayer());
        }
    }

    private static void showArrow(LGPlayer to, LGPlayer ofWho, int entityId) {
        WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
        destroy.setEntityIds(new int[] { entityId });
        destroy.sendPacket(to.getPlayer());
        if(ofWho != null) {
            WrapperPlayServerSpawnEntityLiving spawn = new WrapperPlayServerSpawnEntityLiving();
            spawn.setEntityID(entityId);
            spawn.setType(EntityType.ARMOR_STAND);
            Location loc = ofWho.getPlayer().getLocation();
            spawn.setX(loc.getX());
            spawn.setY(loc.getY() + 1.3);
            spawn.setZ(loc.getZ());
            spawn.setHeadPitch(0);
            Location toLoc = to.getPlayer().getLocation();
            double diffX = loc.getX() - toLoc.getX(),
            diffZ = loc.getZ() - toLoc.getZ();
            float yaw = 180 - ((float) Math.toDegrees(Math.atan2(diffX, diffZ)));

            spawn.setYaw(yaw);
            spawn.sendPacket(to.getPlayer());
            spawn.setMetadata(WrappedDataWatcher.getEntityWatcher(armorStand.getBukkitEntity()));

            WrapperPlayServerEntityLook look = new WrapperPlayServerEntityLook();
            look.setEntityID(entityId);
            look.setPitch(0);
            look.setYaw(yaw);
            look.sendPacket(to.getPlayer());
        }
    }

    public void remove(LGPlayer killed) {
        participants.remove(killed);
        if(!ended) {
            votes.remove(killed);
            latestTop.remove(killed);
        }
    }
}
