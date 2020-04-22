package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGCustomItems;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.localization.Translate;
import lombok.Getter;
import lombok.Setter;

public abstract class Role implements Listener {
    @Getter
    @Setter
    private int waitedPlayers;
    @Getter
    private ArrayList<LGPlayer> players = new ArrayList<LGPlayer>();
    @Getter
    private final LGGame game;
    private final String roleConfigName;
    @Getter
    private final int maxPlayers;

    public Role(LGGame game, int waitedPlayers) {
        this.game = game;
        Bukkit.getPluginManager().registerEvents(this, MainLg.getInstance());
        roleConfigName = "role." + getClass().getSimpleName().substring(1);
        this.waitedPlayers = waitedPlayers;
        this.maxPlayers = waitedPlayers;
    }

    public String getName(LGPlayer player) {
        return roleFormat(player, "name");
    }

    public String getFriendlyName(LGPlayer player) {
        return roleFormat(player, "friendlyname", getName(player));
    }

    public String getShortDescription(LGPlayer player) {
        return roleFormat(player, "shortdesc");
    }

    public String getDescription(LGPlayer player) {
        return roleFormat(player, "desc");
    }

    public String getTask(LGPlayer player) {
        return roleFormat(player, "task");
    }

    public String getBroadcastedTask(LGPlayer player) {
        return roleFormat(player, "taskbroadcast", getName(player));
    }

    public RoleType getType(LGPlayer lgp) {
        return getType();
    }

    public RoleWinType getWinType(LGPlayer lgp) {
        return getWinType();
    }

    public RoleType getType() {
        return null;
    }

    public RoleWinType getWinType() {
        return null;
    }

    /**
     * @return Timeout in second for this role
     */
    public abstract int getTimeout();

    public void onNightTurn(Runnable callback) {
        @SuppressWarnings("unchecked")
        ArrayList<LGPlayer> players = (ArrayList<LGPlayer>) getPlayers().clone();
        new Runnable() {
            @Override
            public void run() {
                getGame().cancelWait();
                if(players.size() == 0) {
                    onTurnFinish(callback);
                    return;
                }
                LGPlayer player = players.remove(0);
                if(player.isRoleActive()) {
                    getGame().wait(getTimeout(), () -> {
                        try {
                            Role.this.onNightTurnTimeout(player);
                        } catch(Exception err) {
                            System.out.println("Error when timeout role");
                            err.printStackTrace();
                        }
                        this.run();
                    }, (currentPlayer, secondsLeft) -> {
                        return currentPlayer == player ? Translate.get(currentPlayer, "role.generic.yourturn") : Translate.get(currentPlayer, "role.generic.othersturn", getFriendlyName(currentPlayer), secondsLeft);
                    });
                    player.sendMessage("§6" + getTask(player));
                    // player.sendTitle("§6C'est à vous de jouer", "§a"+getTask(), 100);
                    onNightTurn(player, this);
                } else {
                    getGame().wait(getTimeout(), ()->{}, (currentPlayer, secondsLeft)->{
                        return currentPlayer == player ? Translate.get(currentPlayer, "role.generic.noturn") : Translate.get(currentPlayer, "role.generic.othersturn", getFriendlyName(currentPlayer), secondsLeft);
                    });
                    Runnable run = this;
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            run.run();
                        }
                    }.runTaskLater(MainLg.getInstance(), 20*(ThreadLocalRandom.current().nextInt(getTimeout()/3*2-4)+4));
                }
            }
        }.run();
    }

    public void join(LGPlayer player, boolean sendMessage) {
        System.out.println(player.getName() + " -> " + getName(player));
        players.add(player);
        if(player.getRole() == null)
            player.setRole(this);
        waitedPlayers--;
        if(sendMessage) {
            player.sendTitle(Translate.get(player, "role.generic.jointitle", getName(player)), "§e" + getShortDescription(player), 200);
            player.sendFormat("role.generic.joinmessage", getName(player));
            player.sendFormat("role.generic.joindescmessage", getDescription(player));
        }
    }

    public void join(LGPlayer player) {
        join(player, !getGame().isStarted());
        LGCustomItems.updateItem(player);
    }

    public boolean hasPlayersLeft() {
        return getPlayers().size() > 0;
    }

    protected void onNightTurnTimeout(LGPlayer player) {
    }

    protected void onNightTurn(LGPlayer player, Runnable callback) {
    }

    protected void onTurnFinish(Runnable callback) {
        callback.run();
    }

    public int getTurnOrder() {
        try {
            RoleSort role = RoleSort.valueOf(getClass().getSimpleName().substring(1));
            return role == null ? -1 : role.ordinal();
        } catch(Throwable e) {
            return -1;
        }
    }// En combientième ce rôle doit être appellé

    public String roleFormat(LGPlayer player, String key) {
        return Translate.get(player, roleConfigName.toLowerCase() + "." + key);
    }

    public String roleFormat(LGPlayer player, String key, Object... args) {
        return Translate.get(player, roleConfigName.toLowerCase() + "." + key, args);
    }
    
    public String roleFormatColor(LGPlayer player, String key) {
        return ChatColor.translateAlternateColorCodes('&', roleFormat(player, key));
    }

    public String roleFormatColor(LGPlayer player, String key, Object... args) {
        return ChatColor.translateAlternateColorCodes('&', roleFormat(player, key, args));
    }

    public Function<LGPlayer, String> roleFormat(String key) {
        return lgp -> roleFormat(lgp, key);
    }

    public Function<LGPlayer, String> roleFormat(String key, Object... args) {
        return lgp -> roleFormat(lgp, key, args);
    }
}
