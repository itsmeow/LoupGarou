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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGCustomItems.LGCustomItemsConstraints;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGCustomItemChangeEvent;
import fr.leomelki.loupgarou.events.LGNightEndEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;

public class RLoupGarouNoir extends Role {

    private Runnable callback;
    private LGPlayer toInfect;

    public RLoupGarouNoir(LGGame game, int amount) {
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
        return 15;
    }

    @Override
    public boolean hasPlayersLeft() {
        return super.hasPlayersLeft() && getGame().getDeaths().containsKey(Reason.LOUP_GAROU);
    }

    public void openInventory(LGPlayer player) {
        inMenu = true;

        player.getPlayer().closeInventory();
        player.getPlayer().openInventory(createInventory(toInfect == null, player));
    }

    private Inventory createInventory(boolean skipOnly, LGPlayer player) {
        Inventory inventory = Bukkit.createInventory(null, 9, "§7Infecter " + toInfect.getName() + " ?");
        inventory.setContents(skipOnly ? skipInventory(player) : regularInventory(player));
        return inventory;
    }

    private ItemStack[] skipInventory(LGPlayer player) {
        ItemStack[] items = new ItemStack[9];
        items[3] = createSkipItem(player);
        return items;
    }

    private ItemStack[] regularInventory(LGPlayer player) {
        ItemStack[] items = new ItemStack[9];
        items[3] = createSkipItem(player);
        items[5] = createInfectItem(player);
        return items;
    }

    private ItemStack createSkipItem(LGPlayer player) {
        ItemStack stack = new ItemStack(Material.SPIDER_EYE);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(roleFormat(player, "gui.skip.name"));
        meta.setLore(Arrays.asList(roleFormat(player, "gui.skip.lore").split("\n")));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createInfectItem(LGPlayer player) {
        ItemStack stack = new ItemStack(Material.ROTTEN_FLESH);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(roleFormat(player, "gui.infect.name"));
        meta.setLore(Arrays.asList(roleFormat(player, "gui.skip.lore").split("\n")));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        toInfect = getGame().getDeaths().get(Reason.LOUP_GAROU);
        if(toInfect.getRoleType() == RoleType.LOUP_GAROU)
            toInfect = null;
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
        // player.sendTitle("§cVous n'infectez personne", "§4Vous avez mis trop de temps
        // à vous décider...", 80);
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
        } else if(item.getType() == Material.ROTTEN_FLESH && toInfect != null) {
            e.setCancelled(true);
            closeInventory(player);
            player.updateInventory();
            closeInventory(player);

            lgp.getCache().set("has_infected", true);
            toInfect.getCache().set("infected", true);
            getPlayers().remove(lgp);
            toInfect.getCache().set("just_infected", true);
            lgp.sendActionBarRoleFormat(this, "infect.actionbar", toInfect.getName());
            lgp.sendRoleFormat(this, "infect.message", toInfect.getName());
            lgp.stopChoosing();
            getGame().getDeaths().remove(Reason.LOUP_GAROU, toInfect);
            lgp.hideView();
            callback.run();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDayStart(LGNightEndEvent e) {
        if(e.getGame() == getGame())
            for(LGPlayer player : getGame().getAlive()) {
                if(player.getCache().getBoolean("just_infected")) {
                    player.getCache().remove("just_infected");
                    player.sendRoleFormat(this, "infect.inform.message");
                    player.sendRoleFormat(this, "infect.inform.message2");
                    for(Role role : getGame().getRoles()) {
                        if(role instanceof RLoupGarou) {
                            if(!player.isDead()) {// Si il n'a pas été tué je ne sais comment
                                role.join(player, false);
                                // player.getPlayer().getInventory().setItemInOffHand(new
                                // ItemStack(LGCustomItems.getItem(player)));
                            }
                        }
                    }

                    for(LGPlayer lgp : getGame().getInGame()) {
                        if(lgp.getRoleType() == RoleType.LOUP_GAROU) {
                            lgp.sendRoleFormat(this, "infect.broadcast.lg", player.getName());
                        } else {
                            lgp.sendRoleFormat(this, "infect.broadcast.other");
                        }
                    }

                    if(getGame().checkEndGame()) {
                        e.setCancelled(true);
                    }
                }
            }
    }

    @Override
    public void join(LGPlayer player, boolean sendMessage) {
        super.join(player, sendMessage);
        for(Role role : getGame().getRoles())
            if(role instanceof RLoupGarou)
                role.join(player, false);
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
    public void onCustomItemChange(LGCustomItemChangeEvent e) {
        if(e.getGame() == getGame())
            if(e.getPlayer().getCache().getBoolean("infected"))
                e.getConstraints().add(LGCustomItemsConstraints.INFECTED.getName());
    }

}
