package fr.leomelki.loupgarou.roles;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventoryCustom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGNightPlayerPreKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGPreDayStartEvent;
import fr.leomelki.loupgarou.utils.VariableCache;

public class RSurvivant extends Role {

    private Runnable callback;

    public RSurvivant(LGGame game) {
        super(game);
    }

    @Override
    public RoleType getType() {
        return RoleType.NEUTRAL;
    }

    @Override
    public RoleWinType getWinType() {
        return RoleWinType.NONE;
    }

    @Override
    public int getTimeout() {
        return 15;
    }

    boolean inMenu;

    public void openInventory(LGPlayer player) {
        inMenu = true;
        player.getPlayer().closeInventory();
        player.getPlayer().openInventory(createInventory(player));
    }

    private Inventory createInventory(LGPlayer player) {
        Inventory inventory = Bukkit.createInventory(null, 9, roleFormat(player, "gui.title"));
        ItemStack[] items = new ItemStack[9];
        VariableCache cache = player.getCache();
        if(cache.<Integer>get("survivant_left") > 0) {
            items[3] = item(player, "nothing", Material.SPIDER_EYE, true);
            {
                items[5] = new ItemStack(Material.GOLD_NUGGET);
                ItemMeta meta = items[5].getItemMeta();
                // special handling case for cache
                meta.setDisplayName(roleFormat(player, "gui.protect.name", cache.<Integer>get("survivant_left")));
                meta.setLore(Arrays.asList(roleFormat(player, "gui.protect.lore").split("\n")));
                items[5].setItemMeta(meta);
            }
        } else {
            items[4] = item(player, "nothing", Material.SPIDER_EYE, true);
        }
        inventory.setContents(items);
        return inventory;
    }

    private ItemStack item(LGPlayer player, String name, Material mat, boolean hasLore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(roleFormat(player, "gui." + name + ".name"));
        if(hasLore) {
            meta.setLore(Arrays.asList(roleFormat(player, "gui." + name + ".lore").split("\n")));
        }
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public void join(LGPlayer player) {
        super.join(player);
        player.getCache().set("survivant_left", 2);
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        player.showView();
        this.callback = callback;
        openInventory(player);
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.hideView();
        closeInventory(player.getPlayer());
        player.sendRoleFormat(this, "timeout");
    }

    private void closeInventory(Player p) {
        inMenu = false;
        p.closeInventory();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();
        Player player = (Player) e.getWhoClicked();
        LGPlayer lgp = LGPlayer.thePlayer(player);

        if(lgp.getRole() != this || item == null || item.getItemMeta() == null)
            return;

        if(item.getType() == Material.SPIDER_EYE) {
            e.setCancelled(true);
            lgp.sendRoleFormat(this, "timeout");
            closeInventory(player);
            lgp.hideView();
            callback.run();
        } else if(item.getType() == Material.GOLD_NUGGET) {
            e.setCancelled(true);
            closeInventory(player);
            lgp.sendActionBarRoleFormat(this, "protect.actionbar");
            lgp.sendRoleFormat(this, "protect.message");
            lgp.getCache().set("survivant_left", lgp.getCache().<Integer>get("survivant_left") - 1);
            lgp.getCache().set("survivant_protected", true);
            lgp.hideView();
            callback.run();
        }
    }

    @EventHandler
    public void onPlayerKill(LGNightPlayerPreKilledEvent e) {
        if(e.getGame() == getGame() && (e.getReason() == Reason.LOUP_GAROU || e.getReason() == Reason.LOUP_BLANC || e.getReason() == Reason.GM_LOUP_GAROU || e.getReason() == Reason.ASSASSIN) && e.getKilled().getCache().getBoolean("survivant_protected")) {
            e.setReason(Reason.DONT_DIE);
        }
    }

    @EventHandler
    public void onDayStart(LGPreDayStartEvent e) {
        if(e.getGame() == getGame())
            for(LGPlayer lgp : getGame().getInGame())
                lgp.getCache().remove("survivant_protected");
    }

    @EventHandler
    public void onQuitInventory(InventoryCloseEvent e) {
        if(e.getInventory() instanceof CraftInventoryCustom) {
            LGPlayer player = LGPlayer.thePlayer((Player) e.getPlayer());
            if(player.getRole() == this && inMenu) {
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        e.getPlayer().openInventory(e.getInventory());
                    }
                }.runTaskLater(MainLg.getInstance(), 1);
            }
        }
    }

    @EventHandler
    public void onWin(LGGameEndEvent e) {
        if(e.getGame() == getGame() && getPlayers().size() > 0 && e.getWinType() != LGWinType.ANGE) {
            for(LGPlayer lgp : getPlayers()) {
                e.getWinners().add(lgp);
            }
            new BukkitRunnable() {

                @Override
                public void run() {
                    getGame().broadcastFunction(lgp -> roleFormat(lgp, "win", getName(lgp)));
                }
            }.runTaskAsynchronously(MainLg.getInstance());
        }
    }
}
