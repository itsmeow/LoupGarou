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

public class RChienLoup extends Role {
    private ItemStack[] items;

    public RChienLoup(LGGame game, int amount) {
        super(game, amount);
        items = new ItemStack[9];
        items[3] = new ItemStack(Material.GOLDEN_APPLE);
        items[5] = new ItemStack(Material.ROTTEN_FLESH);
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
    public boolean hasPlayersLeft() {
        return super.hasPlayersLeft() && !already;
    }

    Runnable callback;
    boolean already;

    public void openInventory(LGPlayer player) {
        inMenu = true;
        Inventory inventory = Bukkit.createInventory(null, 9, roleFormat(player, "gui.title"));
        inventory.setContents(updateMeta(player, items.clone()));
        player.getPlayer().closeInventory();
        player.getPlayer().openInventory(inventory);
    }

    private ItemStack[] updateMeta(LGPlayer player, ItemStack[] clone) {
        ItemMeta meta = clone[3].getItemMeta();
        meta.setDisplayName(roleFormat(player, "gui.villagers.name"));
        meta.setLore(Arrays.asList(roleFormat(player, "gui.villagers.lore").split("\n")));
        clone[3].setItemMeta(meta);

        meta = items[5].getItemMeta();
        meta.setDisplayName(roleFormat(player, "gui.lg.name"));
        meta.setLore(Arrays.asList(roleFormat(player, "gui.lg.lore").split("\n")));
        clone[5].setItemMeta(meta);
        return clone;
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        already = true;
        player.showView();
        this.callback = callback;
        openInventory(player);
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        closeInventory(player.getPlayer());
        player.hideView();
        // player.sendTitle("§cVous n'infectez personne", "§4Vous avez mis trop de temps
        // à vous décider...", 80);
        player.sendActionBarRoleFormat(this, "join.village.actionbar");
        player.sendRoleFormat(this, "join.village.message");
    }

    boolean inMenu;

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

        if(item.getItemMeta().getDisplayName().equals(items[3].getItemMeta().getDisplayName())) {
            e.setCancelled(true);
            closeInventory(player);
            lgp.sendActionBarRoleFormat(this, "remain.villagers.actionbar");
            lgp.sendRoleFormat(this, "remain.villagers.message");
            lgp.hideView();
            callback.run();
        } else if(item.getItemMeta().getDisplayName().equals(items[5].getItemMeta().getDisplayName())) {
            e.setCancelled(true);
            closeInventory(player);

            lgp.sendActionBarRoleFormat(this, "change.actionbar");
            lgp.sendRoleFormat(this, "change.message");

            // On le fait aussi rejoindre le camp des loups pour le tour pendant la nuit.
            RChienLoupLG lgChienLoup = null;
            for(Role role : getGame().getRoles())
                if(role instanceof RChienLoupLG)
                    lgChienLoup = (RChienLoupLG) role;

            if(lgChienLoup == null)
                getGame().getRoles().add(lgChienLoup = new RChienLoupLG(getGame(), this.getMaxPlayers()));

            lgChienLoup.join(lgp, false);
            lgp.updateOwnSkin();

            lgp.hideView();
            callback.run();
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
