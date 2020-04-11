package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventoryCustom;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
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
import fr.leomelki.loupgarou.events.LGDayEndEvent;
import fr.leomelki.loupgarou.events.LGMayorVoteEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.localization.Translate;
import fr.leomelki.loupgarou.events.LGVoteEvent;

public class RDictateur extends Role {

    private Runnable onClick;
    private Runnable onDoNothing;
    private boolean inMenu = false;

    public RDictateur(LGGame game, int amount) {
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
        return 15;
    }

    public void openInventory(LGPlayer player) {
        inMenu = true;
        player.getPlayer().closeInventory();
        player.getPlayer().openInventory(createInventory(player));
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        player.showView();
        this.onClick = callback;
        openInventory(player);
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.hideView();
        closeInventory(player);
        /*
         * player.sendTitle("§cVous ne faites pas votre coup d'état.",
         * "§4Vous avez mis trop de temps à vous décider...", 80);
         * player.sendMessage("§cVous ne faites pas votre coup d'état."); player.
         * sendMessage("§7§oVous aurez de nouveau le choix lors de la prochaine nuit.");
         */
    }

    private void closeInventory(LGPlayer p) {
        inMenu = false;
        p.getPlayer().closeInventory();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        ItemStack item = e.getCurrentItem();
        LGPlayer lgp = LGPlayer.thePlayer((Player) e.getWhoClicked());

        if(lgp.getRole() != this || item == null || item.getItemMeta() == null)
            return;

        if(item.getType() == Material.SPIDER_EYE) {
            e.setCancelled(true);
            closeInventory(lgp);
            /*
             * lgp.sendMessage("§cVous ne faites pas votre coup d'état."); lgp.
             * sendMessage("§7§oVous aurez de nouveau le choix lors de la prochaine nuit.");
             */
            lgp.hideView();
            onClick.run();
        } else if(item.getType() == Material.SULPHUR) {
            e.setCancelled(true);
            closeInventory(lgp);
            lgp.sendActionBarRoleFormat(this, "gui.click.coup.actionbar");
            lgp.sendRoleFormat(this, "gui.click.coup.message");
            lgp.getCache().set("coup_d_etat", true);
            lgp.getCache().set("just_coup_d_etat", true);
            lgp.hideView();
            onClick.run();
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        LGPlayer player = LGPlayer.thePlayer(p);
        if(e.getItem() != null && e.getItem().getType() == Material.SPIDER_EYE && player.getRole() == this) {
            getGame().cancelWait();
            player.stopChoosing();
            p.getInventory().setItem(8, null);
            p.updateInventory();
            getGame().broadcastFunction(lgp -> roleFormat(lgp, "gui.click.none.broadcast", player.getName()));
            onDoNothing.run();
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

    @EventHandler
    public void onMayorVote(LGMayorVoteEvent e) {
        if(e.getGame() == getGame())
            onTurn(e);
    }

    @EventHandler
    public void onVote(LGVoteEvent e) {
        if(e.getGame() == getGame())
            onTurn(e);
    }

    public void onTurn(Cancellable e) {
        for(LGPlayer lgp : getPlayers())
            if(lgp.getCache().getBoolean("just_coup_d_etat") && lgp.isRoleActive())
                e.setCancelled(true);

        if(!e.isCancelled())
            return;

        @SuppressWarnings("unchecked")
        Iterator<LGPlayer> ite = ((ArrayList<LGPlayer>) getPlayers().clone()).iterator();
        new Runnable() {
            public void run() {
                onDoNothing = this;
                if(ite.hasNext()) {
                    LGPlayer lgp = ite.next();
                    if(lgp.getCache().getBoolean("just_coup_d_etat")) {
                        getPlayers().remove(lgp);
                        lgp.getCache().remove("just_coup_d_etat");
                        getGame().broadcastFunction(roleFormat("coup.broadcast", lgp.getName()));
                        // lgp.sendTitle("§6Vous faites votre coup d'état", "§aChoisissez qui tuer",
                        // 60);

                        // On le met sur le slot 0 pour éviter un missclick sur la croix
                        WrapperPlayServerHeldItemSlot hold = new WrapperPlayServerHeldItemSlot();
                        hold.setSlot(0);
                        hold.sendPacket(lgp.getPlayer());

                        lgp.sendRoleFormat(RDictateur.this, "coup.message");
                        getGame().wait(60, () -> {
                            lgp.stopChoosing();
                            getGame().broadcastFunction(roleFormat("coup.timeout", lgp.getName()));
                            lgp.getPlayer().getInventory().setItem(8, null);
                            lgp.getPlayer().updateInventory();
                            this.run();
                        }, (player, secondsLeft) -> {
                            return lgp == player ? Translate.get(player, "role.generic.yourturn") : roleFormat(player, "othersturn", secondsLeft);
                        });
                        lgp.choose((choosen) -> {
                            if(choosen != null) {
                                getGame().cancelWait();
                                lgp.stopChoosing();
                                lgp.getPlayer().getInventory().setItem(8, null);
                                lgp.getPlayer().updateInventory();
                                kill(choosen, lgp, this);
                            }
                        });
                        lgp.getPlayer().getInventory().setItem(8, createNothingItem(lgp));
                        lgp.getPlayer().updateInventory();
                    }
                } else
                    getGame().nextNight();
            }
        }.run();
    }
    
    private Inventory createInventory(LGPlayer player) {
        ItemStack[] items = new ItemStack[9];
        items[3] = createNothingItem(player);
        items[5] = createCoupItem(player);
        Inventory inventory = Bukkit.createInventory(null, 9, roleFormat(player, "gui.title"));

        inventory.setContents(items.clone());
        return inventory;
    }

    private ItemStack createNothingItem(LGPlayer player) {
        ItemStack stack = new ItemStack(Material.SPIDER_EYE);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(roleFormat(player, "gui.none.name"));
        meta.setLore(Arrays.asList(roleFormat(player, "gui.none.lore").split("\n")));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createCoupItem(LGPlayer player) {
        ItemStack stack = new ItemStack(Material.SULPHUR);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(roleFormat(player, "gui.coup.name"));
        meta.setLore(Arrays.asList(roleFormat(player, "gui.coup.lore").split("\n")));
        stack.setItemMeta(meta);
        return stack;
    }

    protected void kill(LGPlayer choosen, LGPlayer dicta, Runnable callback) {
        RoleType roleType = choosen.getRoleType();

        LGPlayerKilledEvent killEvent = new LGPlayerKilledEvent(getGame(), choosen, Reason.DICTATOR);
        Bukkit.getPluginManager().callEvent(killEvent);
        if(killEvent.isCancelled())
            return;
        if(getGame().kill(killEvent.getKilled(), killEvent.getReason(), true))
            return;

        if(roleType != RoleType.VILLAGER) {
            getGame().broadcastFunction(roleFormat("coup.success", dicta.getName()));
            getGame().setMayor(dicta);
        } else {
            getGame().kill(dicta, Reason.DICTATOR_SUICIDE);
            for(LGPlayer lgp : getGame().getInGame()) {
                if(lgp == dicta)
                    lgp.sendRoleFormat(this, "coup.fail.message");
                else
                    lgp.sendRoleFormat(this, "coup.fail.broadcast", getName(lgp));
            }
        }
        callback.run();
    }

    @EventHandler
    public void onNight(LGDayEndEvent e) {
        if(e.getGame() == getGame()) {
            LGPlayer lgp = getGame().getDeaths().get(Reason.DICTATOR_SUICIDE);
            if(lgp != null)
                lgp.sendRoleFormat(this, "suicide");
        }
    }
}
