package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.events.LGEndCheckEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;
import fr.leomelki.loupgarou.events.LGPyromaneGasoilEvent;

public class RPyromane extends Role {

    private Runnable callback;

    public RPyromane(LGGame game) {
        super(game);
    }

    @Override
    public RoleType getType() {
        return RoleType.NEUTRAL;
    }

    @Override
    public RoleWinType getWinType() {
        return RoleWinType.SEUL;
    }

    @Override
    public int getTimeout() {
        return 30;
    }

    public void openInventory(LGPlayer player) {
        inMenu = true;
        player.getPlayer().closeInventory();
        player.getPlayer().openInventory(createInventory(player));
    }

    private Inventory createInventory(LGPlayer player) {
        Inventory inventory = Bukkit.createInventory(null, 9, roleFormat(player, "gui.title"));
        ItemStack[] content = new ItemStack[9];
        content[3] = createFireItem(player);
        content[5] = createGasolineItem(player);
        if(!player.getCache().has("pyromane_essence")) {
            player.getCache().set("pyromane_essence", new ArrayList<>());
        }
        if(player.getCache().<List<LGPlayer>>get("pyromane_essence").size() == 0) {
            content[3] = createNothingItem(player);
        }
        inventory.setContents(content);
        return inventory;
    }

    private ItemStack createCancelItem(LGPlayer player) {
        return item(player, "cancel", Material.SPIDER_EYE, true);
    }

    private ItemStack createNothingItem(LGPlayer player) {
        return item(player, "nothing", Material.SPIDER_EYE, false);
    }

    private ItemStack createFireItem(LGPlayer player) {
        return item(player, "fire", Material.FLINT_AND_STEEL, true);
    }

    private ItemStack createGasolineItem(LGPlayer player) {
        return item(player, "gas", Material.LAVA_BUCKET, true);
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
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        first = null;
        player.showView();
        this.callback = callback;
        openInventory(player);
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        if(first != null) {
            List<LGPlayer> liste = player.getCache().<List<LGPlayer>>get("pyromane_essence");
            LGPyromaneGasoilEvent event = new LGPyromaneGasoilEvent(getGame(), first);
            Bukkit.getPluginManager().callEvent(event);
            if(event.isCancelled()) {
                player.sendRoleFormat(this, "gas.immune", event.getPlayer().getName());
            } else {
                event.getPlayer().sendRoleFormat(this, "gas.inform");
                liste.add(event.getPlayer());
            }
        }
        player.getPlayer().getInventory().setItem(8, null);
        player.stopChoosing();
        closeInventory(player.getPlayer());
        player.getPlayer().updateInventory();
        player.hideView();
        player.sendRoleFormat(this, "timeout");
    }

    boolean inMenu = false;
    LGPlayer first;

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
            lgp.stopChoosing();
            closeInventory(player);
            lgp.hideView();
            lgp.sendRoleFormat(this, "timeout");
            callback.run();
        } else if(item.getType() == Material.FLINT_AND_STEEL) {
            e.setCancelled(true);
            closeInventory(player);
            if(lgp.getCache().<List<LGPlayer>>get("pyromane_essence").size() != 0) {
                List<LGPlayer> liste = lgp.getCache().<List<LGPlayer>>get("pyromane_essence");
                for(LGPlayer scndPlayer : liste) {
                    if(!scndPlayer.isDead() && scndPlayer.getPlayer() != null) {
                        getGame().kill(scndPlayer, Reason.PYROMANE);
                    }
                }
                liste.clear();
                lgp.sendRoleFormat(this, "burn.message");
                lgp.sendActionBarRoleFormat(this, "burn.actionbar");
            } else {
                lgp.sendRoleFormat(this, "burn.none");
            }
            lgp.hideView();
            callback.run();
        } else if(item.getType() == Material.LAVA_BUCKET) {
            e.setCancelled(true);
            closeInventory(player);
            player.getInventory().setItem(8, createCancelItem(lgp));
            player.updateInventory();
            // Pour éviter les missclick
            WrapperPlayServerHeldItemSlot held = new WrapperPlayServerHeldItemSlot();
            held.setSlot(0);
            held.sendPacket(player);
            lgp.sendRoleFormat(this, "gas.choose");
            lgp.choose(new LGChooseCallback() {
                @Override
                public void callback(LGPlayer choosen) {
                    if(choosen != null) {
                        if(choosen == first) {
                            lgp.sendRoleFormat(RPyromane.this, "choose.same", choosen.getName());
                            return;
                        }
                        List<LGPlayer> liste = lgp.getCache().<List<LGPlayer>>get("pyromane_essence");
                        if(liste.contains(choosen)) {
                            lgp.sendRoleFormat(RPyromane.this, "choose.covered", choosen.getName());
                            return;
                        }
                        // not sure why this is here? leomelki wyd
                        /*
                         * if(first == choosen) { lgp.sendMessage("§cVous avez déjà sélectionné §7§l" +
                         * choosen.getName() + "§c."); return; }
                         */
                        player.getInventory().setItem(8, null);
                        player.updateInventory();
                        lgp.sendRoleFormat(RPyromane.this, "gas.message", choosen.getName());
                        lgp.sendActionBarRoleFormat(RPyromane.this, "gas.actionbar", choosen.getName());
                        if(first != null || getGame().getAlive().size() == 2) {
                            lgp.hideView();
                            lgp.stopChoosing();
                            LGPyromaneGasoilEvent event = new LGPyromaneGasoilEvent(getGame(), choosen);
                            Bukkit.getPluginManager().callEvent(event);
                            if(event.isCancelled()) {
                                lgp.sendRoleFormat(RPyromane.this, "gas.immune", event.getPlayer().getName());
                            } else {
                                event.getPlayer().sendRoleFormat(RPyromane.this, "gas.inform");
                                liste.add(event.getPlayer());
                            }
                            if(first != null) {
                                event = new LGPyromaneGasoilEvent(getGame(), first);
                                Bukkit.getPluginManager().callEvent(event);
                                if(event.isCancelled()) {
                                    lgp.sendRoleFormat(RPyromane.this, "gas.immune", event.getPlayer().getName());
                                } else {
                                    event.getPlayer().sendRoleFormat(RPyromane.this, "gas.inform");
                                    liste.add(event.getPlayer());
                                }
                            }
                            callback.run();
                        } else {
                            lgp.sendRoleFormat(RPyromane.this, "gas.choose2");
                            first = choosen;
                        }
                    }
                }
            }, lgp);
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
                openInventory(lgp);
            }
        }
    }

    @EventHandler
    public void onKilled(LGPlayerKilledEvent e) {
        if(e.getGame() == getGame()) {
            for(LGPlayer lgp : getPlayers()) {
                if(lgp.getCache().has("pyromane_essence")) {
                    List<LGPlayer> liste = lgp.getCache().<List<LGPlayer>>get("pyromane_essence");
                    if(liste.contains(e.getKilled()))// Au cas où le mec soit rez
                        liste.remove(e.getKilled());
                }
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

    // Win condition

    @EventHandler
    public void onEndgameCheck(LGEndCheckEvent e) {
        if(e.getGame() == getGame() && e.getWinType() == LGWinType.SOLO) {
            if(getPlayers().size() > 0)
                e.setWinType(LGWinType.PYROMANE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEndGame(LGGameEndEvent e) {
        if(e.getWinType() == LGWinType.PYROMANE) {
            e.getWinners().clear();
            e.getWinners().addAll(getPlayers());
        }
    }

}
