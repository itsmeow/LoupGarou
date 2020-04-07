package fr.leomelki.loupgarou.roles;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.leomelki.loupgarou.classes.LGGame;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.classes.LGPlayer.LGChooseCallback;
import fr.leomelki.loupgarou.classes.LGWinType;
import fr.leomelki.loupgarou.events.LGEndCheckEvent;
import fr.leomelki.loupgarou.events.LGGameEndEvent;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;

public class RLoupGarouBlanc extends Role {

    private Runnable callback;

    public RLoupGarouBlanc(LGGame game) {
        super(game);
    }

    @Override
    public RoleType getType() {
        return RoleType.LOUP_GAROU;
    }

    @Override
    public RoleWinType getWinType() {
        return RoleWinType.SEUL;
    }

    @Override
    public int getTimeout() {
        return 15;
    }

    @Override
    public boolean hasPlayersLeft() {
        return super.hasPlayersLeft() && getGame().getNight() % 2 == 0;
    }

    private ItemStack getSkipItem(LGPlayer player) {
        ItemStack skip = new ItemStack(Material.SPIDER_EYE);
        ItemMeta meta = skip.getItemMeta();
        meta.setDisplayName(roleFormat(player, "gui.skip.name"));
        meta.setLore(Arrays.asList(roleFormat(player, "gui.skip.lore").split("\n")));
        skip.setItemMeta(meta);
        return skip;
    }

    @Override
    protected void onNightTurn(LGPlayer player, Runnable callback) {
        this.callback = callback;
        RLoupGarou lg_ = null;
        for(Role role : getGame().getRoles())
            if(role instanceof RLoupGarou) {
                lg_ = (RLoupGarou) role;
                break;
            }

        RLoupGarou lg = lg_;
        player.showView();
        player.getPlayer().getInventory().setItem(8, getSkipItem(player));
        player.choose(new LGChooseCallback() {
            @Override
            public void callback(LGPlayer choosen) {
                if(choosen != null && choosen != player) {
                    if(!lg.getPlayers().contains(choosen)) {
                        player.sendRoleFormat(RLoupGarouBlanc.this, "choose.notlg", choosen.getName());
                        return;
                    }
                    player.sendActionBarRoleFormat(RLoupGarouBlanc.this, "choose.message", choosen.getName());
                    player.sendRoleFormat(RLoupGarouBlanc.this, "choose.actionbar", choosen.getName());
                    player.getPlayer().getInventory().setItem(8, null);
                    player.getPlayer().updateInventory();
                    getGame().kill(choosen, Reason.LOUP_BLANC);
                    player.stopChoosing();
                    player.hideView();
                    callback.run();
                }
            }
        });
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        LGPlayer player = LGPlayer.thePlayer(p);
        if(e.getItem() != null && e.getItem().getType() == Material.SPIDER_EYE && player.getRole() == this) {
            player.stopChoosing();
            p.getInventory().setItem(8, null);
            p.updateInventory();
            player.hideView();
            player.sendRoleFormat(this, "pass");
            callback.run();
        }
    }

    @Override
    protected void onNightTurnTimeout(LGPlayer player) {
        player.stopChoosing();
        player.getPlayer().getInventory().setItem(8, null);
        player.getPlayer().updateInventory();
        player.hideView();
        player.sendRoleFormat(this, "pass");
    }

    RLoupGarou lg;

    @Override
    public void join(LGPlayer player, boolean sendMessage) {
        super.join(player, sendMessage);
        for(Role role : getGame().getRoles())
            if(role instanceof RLoupGarou)
                (lg = (RLoupGarou) role).join(player, false);
    }

    @EventHandler
    public void onEndgameCheck(LGEndCheckEvent e) {
        if(e.getGame() == getGame() && e.getWinType() == LGWinType.SOLO) {
            if(getPlayers().size() > 0) {
                if(lg.getPlayers().size() > getPlayers().size())
                    e.setWinType(LGWinType.NONE);
                else if(lg.getPlayers().size() == getPlayers().size())
                    e.setWinType(LGWinType.LOUPGAROUBLANC);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEndGame(LGGameEndEvent e) {
        if(e.getWinType() == LGWinType.LOUPGAROUBLANC) {
            e.getWinners().clear();
            e.getWinners().addAll(getPlayers());
        }
    }

}
