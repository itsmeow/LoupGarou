package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;

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

    public Role(LGGame game) {
        this.game = game;
        Bukkit.getPluginManager().registerEvents(this, MainLg.getInstance());
        FileConfiguration config = MainLg.getInstance().getConfig();
        roleConfigName = "role." + getClass().getSimpleName().substring(1);
        if(config.contains(roleConfigName))
            waitedPlayers = config.getInt(roleConfigName);
    }

    public final String getName() {
        return Translate.getRaw("role." + roleConfigName + ".name");
    }

    public final String getFriendlyName() {
        return Translate.getFormatted("role.friendlyname", getName());
    }

    public final String getShortDescription() {
        return Translate.getRaw("role." + roleConfigName + ".shortdesc");
    }

    public final String getDescription() {
        return Translate.getRaw("role." + roleConfigName + ".desc");
    }

    public final String getTask() {
        return Translate.getRaw("role." + roleConfigName + ".task");
    }

    public final String getBroadcastedTask() {
        return Translate.getRaw("role." + roleConfigName + ".taskbroadcast");
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
                getGame().wait(getTimeout(), () -> {
                    try {
                        Role.this.onNightTurnTimeout(player);
                    } catch(Exception err) {
                        System.out.println("Error when timeout role");
                        err.printStackTrace();
                    }
                    this.run();
                }, (currentPlayer, secondsLeft) -> {
                    return currentPlayer == player ? Translate.getRaw("role.yourturn") : Translate.getFormatted("role.othersturn", getFriendlyName(), secondsLeft);
                });
                player.sendMessage("§6" + getTask());
                // player.sendTitle("§6C'est à vous de jouer", "§a"+getTask(), 100);
                onNightTurn(player, this);
            }
        }.run();
    }

    public void join(LGPlayer player, boolean sendMessage) {
        System.out.println(player.getName() + " -> " + getName());
        players.add(player);
        if(player.getRole() == null)
            player.setRole(this);
        waitedPlayers--;
        if(sendMessage) {
            player.sendTitle(Translate.getFormatted("role.jointitle", getName()), "§e" + getShortDescription(), 200);
            player.sendMessage(Translate.getFormatted("role.joinmessage", getName()));
            player.sendMessage(Translate.getFormatted("role.joindescmessage", getDescription()));
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
}
