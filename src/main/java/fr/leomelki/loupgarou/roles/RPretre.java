package fr.leomelki.loupgarou.roles;

import java.util.ArrayList;
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
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.wrappers.WrappedWatchableObject;

import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerEntityMetadata;
import fr.leomelki.com.comphenix.packetwrapper.WrapperPlayServerHeldItemSlot;
import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGCustomItems;
import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.events.LGPreDayStartEvent;
import fr.leomelki.loupgarou.utils.VariousUtils;

public class RPretre extends Role {

    private Runnable callback;
    private boolean inMenu = false;
    private ArrayList<LGPlayer> ressucited = new ArrayList<LGPlayer>();

    public RPretre(LGGame game, int amount) {
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

    @Override
    public boolean hasPlayersLeft() {
        for(LGPlayer pretre : getPlayers())
            for(LGPlayer lgp : getGame().getInGame())
                if(lgp.isDead() && (lgp.getRoleType() == RoleType.VILLAGER || lgp.getRoleType() == pretre.getRoleType()))
                    return super.hasPlayersLeft();
        return false;
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
        items[5] = createReviveItem(player);
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

    private ItemStack createReviveItem(LGPlayer player) {
        ItemStack stack = new ItemStack(Material.ROTTEN_FLESH);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(roleFormat(player, "gui.revive.name"));
        meta.setLore(Arrays.asList(roleFormat(player, "gui.revive.lore").split("\n")));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        player.showView();
        for(LGPlayer lgp : getGame().getInGame()) {
            if(lgp.isDead() && (lgp.getRoleType() == RoleType.VILLAGER || lgp.getRoleType() == player.getRoleType())) {
                if(lgp.getPlayer() != null) {
                    player.getPlayer().showPlayer(lgp.getPlayer());
                    WrapperPlayServerEntityMetadata meta = new WrapperPlayServerEntityMetadata();
                    meta.setEntityID(lgp.getPlayer().getEntityId());
                    meta.setMetadata(Arrays.asList(new WrappedWatchableObject(0, (byte) 0))); // set not invisible
                    meta.sendPacket(player.getPlayer());
                }
            } else {
                player.getPlayer().hidePlayer(lgp.getPlayer());
            }
        }
        this.callback = callback;
        openInventory(player);
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.getPlayer().getInventory().setItem(8, null);
        player.stopChoosing();
        closeInventory(player.getPlayer());
        player.canSelectDead = false;
        player.getPlayer().updateInventory();
        hidePlayers(player);
        player.sendRoleFormat(this, "timeout");
    }

    private void hidePlayers(LGPlayer player) {
        if(player.getPlayer() != null) {
            for(LGPlayer lgp : getGame().getInGame()) {
                if(lgp.getPlayer() != null && lgp != player) {
                    player.getPlayer().hidePlayer(lgp.getPlayer());
                }
            }
        }
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
            closeInventory(player);
            lgp.sendRoleFormat(this, "timeout");
            hidePlayers(lgp);
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
            lgp.sendRoleFormat(this, "choose.message");
            lgp.canSelectDead = true;
            lgp.choose(new LGChooseCallback() {

                @Override
                public void callback(LGPlayer choosen) {
                    if(choosen != null) {
                        if(!choosen.isDead())
                            lgp.sendRoleFormat(RPretre.this, "revive.living", choosen.getName());
                        else if(lgp.getRoleType() == RoleType.LOUP_GAROU && choosen.getRoleType() == RoleType.NEUTRAL) {
                            lgp.sendRoleFormat(RPretre.this, "revive.neutral", choosen.getName());
                        } else if(lgp.getRoleType() != RoleType.LOUP_GAROU && choosen.getRoleType() != RoleType.VILLAGER) {
                            lgp.sendRoleFormat(RPretre.this, "revive.nonmember", choosen.getName());
                        } else {
                            player.getInventory().setItem(8, null);
                            player.updateInventory();
                            lgp.stopChoosing();
                            lgp.canSelectDead = false;
                            lgp.sendRoleFormat(RPretre.this, "revive.message", choosen.getName());
                            lgp.sendActionBarRoleFormat(RPretre.this, "revive.actionbar", choosen.getName());

                            ressucited.add(choosen);
                            getPlayers().remove(lgp);// Pour éviter qu'il puisse sauver plusieurs personnes.
                            choosen.sendRoleFormat(RPretre.this, "revive.inform");
                            hidePlayers(lgp);
                            lgp.hideView();
                            callback.run();
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
                lgp.sendRoleFormat(this, "timeout");
                lgp.canSelectDead = false;
                hidePlayers(lgp);
                callback.run();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDayStart(LGPreDayStartEvent e) {
        if(e.getGame() == getGame())
            if(ressucited.size() > 0) {
                for(LGPlayer lgp : ressucited) {
                    if(lgp.getPlayer() == null || !lgp.isDead())
                        continue;
                    lgp.setDead(false);
                    lgp.getCache().reset();
                    RVillageois villagers = null;
                    for(Role role : getGame().getRoles())
                        if(role instanceof RVillageois)
                            villagers = (RVillageois) role;
                    if(villagers == null)
                        getGame().getRoles().add(villagers = new RVillageois(getGame(), 1));
                    villagers.join(lgp, false);// Le joueur réssuscité rejoint les villageois.
                    lgp.setRole(villagers);
                    lgp.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
                    lgp.getPlayer().getInventory().setHelmet(null);
                    lgp.getPlayer().updateInventory();
                    LGCustomItems.updateItem(lgp);

                    lgp.joinChat(getGame().getDayChat());// Pour qu'il ne parle plus dans le chat des morts (et ne le voit plus) et qu'il parle dans le chat des vivants
                    VariousUtils.setWarning(lgp.getPlayer(), true);

                    getGame().updateRoleScoreboard();

                    getGame().broadcastFunction(roleFormat("revive.broadcast", lgp.getName()));

                    for(LGPlayer player : getGame().getInGame())
                        if(player.getPlayer() != null && player != lgp) {
                            player.getPlayer().showPlayer(lgp.getPlayer());
                        }
                }
                ressucited.clear();
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
