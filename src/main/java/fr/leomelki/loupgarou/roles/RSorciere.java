package fr.leomelki.loupgarou.roles;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventoryCustom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Dye;
import org.bukkit.scheduler.BukkitRunnable;

import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerHeldItemSlot;
import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;

public class RSorciere extends Role {

    private boolean inMenu = false;

    public RSorciere(LGGame game, int amount) {
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
        return 30;
    }

    private LGPlayer sauver;
    private Runnable callback;

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        player.showView();
        this.callback = callback;
        sauver = getGame().getDeaths().get(Reason.LOUP_GAROU);
        if(sauver == null)
            sauver = getGame().getDeaths().get(Reason.DONT_DIE);

        openInventory(player);
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.getPlayer().getInventory().setItem(8, null);
        player.stopChoosing();
        closeInventory(player.getPlayer());
        player.getPlayer().updateInventory();
        player.hideView();
    }

    private void openInventory(LGPlayer player) {
        inMenu = true;
        player.getPlayer().closeInventory();
        player.getPlayer().openInventory(createInventory(player));
    }

    private Inventory createInventory(LGPlayer player) {
        Inventory inventory = Bukkit.createInventory(null, InventoryType.BREWING, sauver == null ? roleFormatColor(player, "gui.notarget") : roleFormatColor(player, "gui.target", sauver.getName()));
        ItemStack[] items = new ItemStack[4];
        items[0] = createLifePotion(player);
        items[1] = createNothingItem(player);
        items[2] = createDeathPotion(player);
        inventory.setContents(items);
        if(sauver == null || player.getCache().getBoolean("witch_used_life")) {
            inventory.setItem(0, null);
        }
        if(sauver != null) {
            inventory.setItem(3, createTargetItem(player));
        }
        if(player.getCache().getBoolean("witch_used_death")) {
            inventory.setItem(2, null);
        }
        return inventory;
    }

    private ItemStack createLifePotion(LGPlayer player) {
        return dye(player, "lifepotion", DyeColor.PURPLE, true);
    }

    private ItemStack createDeathPotion(LGPlayer player) {
        return dye(player, "deathpotion", DyeColor.LIGHT_BLUE, true);
    }

    private ItemStack createNothingItem(LGPlayer player) {
        return item(player, "nothing", Material.SPIDER_EYE, false);
    }

    private ItemStack createCancelItem(LGPlayer player) {
        return item(player, "cancel", Material.SPIDER_EYE, false);
    }

    private ItemStack createTargetItem(LGPlayer player) {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(sauver != null ? roleFormatColor(player, "gui.target", sauver.getName()) : roleFormatColor(player, "gui.notarget", sauver.getName()));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack item(LGPlayer player, String name, Material mat, boolean hasLore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(roleFormatColor(player, "gui." + name + ".name"));
        if(hasLore) {
            meta.setLore(Arrays.asList(roleFormatColor(player, "gui." + name + ".lore").split("\n")));
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack dye(LGPlayer player, String name, DyeColor dyeC, boolean hasLore) {
        Dye dye = new Dye();
        dye.setColor(dyeC);
        ItemStack stack = dye.toItemStack();
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(roleFormatColor(player, "gui." + name + ".name"));
        if(hasLore) {
            meta.setLore(Arrays.asList(roleFormatColor(player, "gui." + name + ".lore").split("\n")));
        }
        stack.setItemMeta(meta);
        return stack;
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

        if(e.getSlot() == 0 && item.getType() == Material.INK_SACK && sauver != null) {// Potion de vie
            e.setCancelled(true);
            closeInventory(player);
            saveLife(lgp);
        } else if(item.getType() == Material.SPIDER_EYE) {// Cancel
            e.setCancelled(true);
            closeInventory(player);
            lgp.sendRoleFormat(this, "timeout");
            lgp.hideView();
            callback.run();
        } else if(e.getSlot() == 2 && item.getType() == Material.INK_SACK) {// Potion de mort
            e.setCancelled(true);
            player.getInventory().setItem(8, createCancelItem(lgp));
            player.updateInventory();

            // On le met sur le slot 0 pour éviter un missclick sur la croix
            WrapperPlayServerHeldItemSlot hold = new WrapperPlayServerHeldItemSlot();
            hold.setSlot(0);
            hold.sendPacket(lgp.getPlayer());

            closeInventory(player);
            lgp.choose((choosen) -> {
                if(choosen != null) {
                    lgp.stopChoosing();
                    kill(choosen, lgp);
                }
            }/* , sauver */);
            // On peut tuer la personne qui a été tué par les loups (bien que cela ne serve
            // à rien)
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        LGPlayer player = LGPlayer.thePlayer(p);
        if(e.getItem() != null && e.getItem().getType() == Material.SPIDER_EYE && player.getRole() == this) {
            player.stopChoosing();
            p.getInventory().setItem(8, null);
            p.updateInventory();

            openInventory(player);
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

    private void kill(LGPlayer choosen, LGPlayer player) {
        player.getPlayer().getInventory().setItem(8, null);
        player.getPlayer().updateInventory();
        player.getCache().set("witch_used_death", true);
        getGame().kill(choosen, Reason.SORCIERE);
        player.sendRoleFormat(this, "kill.message", choosen.getName());
        player.sendActionBarRoleFormat(this, "kill.actionbar", choosen.getName());
        player.hideView();
        callback.run();
    }

    private void saveLife(LGPlayer player) {
        player.getCache().set("witch_used_life", true);
        getGame().getDeaths().remove(Reason.LOUP_GAROU, sauver);
        player.sendRoleFormat(this, "save.message", sauver.getName());
        player.sendActionBarRoleFormat(this, "save.actionbar", sauver.getName());
        player.hideView();
        callback.run();
    }
}
