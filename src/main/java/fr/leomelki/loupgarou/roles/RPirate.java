package fr.leomelki.loupgarou.roles;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventoryCustom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerHeldItemSlot;
import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;

public class RPirate extends Role {

    private Runnable callback;

    public RPirate(LGGame game) {
        super(game);
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

    public void openInventory(LGPlayer player) {
        inMenu = true;

        player.getPlayer().closeInventory();
        player.getPlayer().openInventory(createInventory(player));
    }

    private Inventory createInventory(LGPlayer player) {
        Inventory inventory = Bukkit.createInventory(null, 9, roleFormat(player, "gui.title"));
        ItemStack[] items = new ItemStack[9];
        items[3] = createSkipItem(player);
        items[5] = createHostageItem(player);
        inventory.setContents(items);
        return inventory;
    }

    private ItemStack createSkipItem(LGPlayer player) {
        ItemStack stack = new ItemStack(Material.SPIDER_EYE);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(roleFormat(player, "gui.skip.name"));
        meta.setLore(Arrays.asList(roleFormat(player, "gui.skip.lore").split("\n")));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createHostageItem(LGPlayer player) {
        ItemStack stack = new ItemStack(Material.ROTTEN_FLESH);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(roleFormat(player, "gui.hostage.name"));
        meta.setLore(Arrays.asList(roleFormat(player, "gui.hostage.lore").split("\n")));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        player.showView();
        this.callback = callback;
        openInventory(player);
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.getPlayer().getInventory().setItem(8, null);
        player.stopChoosing();
        closeInventory(player.getPlayer());
        player.getPlayer().updateInventory();
        player.hideView();
        player.sendRoleFormat(this, "timeout");
    }

    boolean inMenu = false;

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
            closeInventory(player);
            lgp.sendRoleFormat(this, "timeout");
            lgp.hideView();
            callback.run();
        } else if(item.getType() == Material.ROTTEN_FLESH) {
            e.setCancelled(true);
            closeInventory(player);
            player.getInventory().setItem(8, createSkipItem(lgp));
            player.updateInventory();
            // Pour éviter les missclick
            WrapperPlayServerHeldItemSlot held = new WrapperPlayServerHeldItemSlot();
            held.setSlot(0);
            held.sendPacket(player);
            lgp.sendRoleFormat(this, "gui.click.hostage");
            lgp.choose(new LGChooseCallback() {

                @Override
                public void callback(LGPlayer choosen) {
                    if(choosen != null) {
                        player.getInventory().setItem(8, null);
                        player.updateInventory();
                        lgp.stopChoosing();
                        lgp.sendRoleFormat(RPirate.this, "choose.message", choosen.getName());
                        lgp.sendActionBarRoleFormat(RPirate.this, "choose.actionbar", choosen.getName());
                        lgp.getCache().set("pirate_otage", choosen);
                        choosen.getCache().set("pirate_otage_d", lgp);
                        getPlayers().remove(lgp);// Pour éviter qu'il puisse prendre plusieurs otages
                        choosen.sendRoleFormat(RPirate.this, "choose.inform", lgp.getName(), getName(choosen));
                        lgp.hideView();
                        callback.run();
                    }
                }
            }, lgp);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerKilled(LGPlayerKilledEvent e) {
        if(e.getGame() == getGame() && e.getReason() == Reason.VOTE)
            if(e.getKilled().getCache().has("pirate_otage")) {
                LGPlayer otage = e.getKilled().getCache().remove("pirate_otage");
                if(!otage.isDead() && otage.getCache().get("pirate_otage_d") == e.getKilled()) {
                    getGame().broadcastFunction(lgp -> roleFormat(lgp, "death.hostage.broadcast", e.getKilled().getName(), getName(lgp)));
                    e.setKilled(otage);
                    e.setReason(Reason.PIRATE);
                }
            }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        LGPlayer lgp = LGPlayer.thePlayer(player);
        if(lgp.getRole() == this) {
            if(e.getItem() != null && e.getItem().getType() == Material.SPIDER_EYE) {
                e.setCancelled(true);
                player.getInventory().setItem(8, null);
                player.updateInventory();
                lgp.stopChoosing();
                lgp.sendRoleFormat(this, "timeout");
                lgp.hideView();
                callback.run();
            }
        }
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

}
