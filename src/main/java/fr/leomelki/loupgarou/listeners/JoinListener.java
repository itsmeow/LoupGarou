package fr.leomelki.loupgarou.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

import fr.leomelki.loupgarou.MainLg;
import fr.leomelki.loupgarou.classes.LGPlayer;
import fr.leomelki.loupgarou.events.LGPlayerKilledEvent.Reason;

public class JoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        onJoin(e.getPlayer(), e.getJoinMessage());
        e.setJoinMessage("");
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        onLeave(e.getPlayer());
    }

    public static void onLeave(Player p) {
        LGPlayer lgp = LGPlayer.thePlayer(p);
        if(lgp.getGame() != null) {
            lgp.leaveChat();
            if(lgp.getRole() != null && !lgp.isDead())
                lgp.getGame().kill(lgp, Reason.DISCONNECTED, true);
            lgp.getGame().getInGame().remove(lgp);
            lgp.getGame().checkLeave();
        }
        LGPlayer.removePlayer(p);
        lgp.remove();
    }

    public static void onJoin(Player p, String msg) {
        /*WrapperPlayServerScoreboardTeam myTeam = new WrapperPlayServerScoreboardTeam();
        myTeam.setName(p.getName());
        myTeam.setPrefix("");
        myTeam.setPlayers(Arrays.asList(p.getName()));
        myTeam.setMode(0);*/
        boolean noSpec = p.getGameMode() != GameMode.SPECTATOR;
        for(Player player : Bukkit.getOnlinePlayers()) {
            if(player != p) {
                if(player.getGameMode() != GameMode.SPECTATOR)
                    player.hidePlayer(p);
                /*WrapperPlayServerScoreboardTeam team = new WrapperPlayServerScoreboardTeam();
                team.setName(player.getName());
                team.setPrefix("");
                team.setPlayers(Arrays.asList(player.getName()));
                team.setMode(0);

                team.sendPacket(p);
                myTeam.sendPacket(player);*/
            }
        }
        p.setFoodLevel(6);
        LGPlayer lgp = LGPlayer.thePlayer(p);
        lgp.showView();
        lgp.join(MainLg.getInstance().getCurrentGame());
        if(noSpec)
            p.setGameMode(GameMode.ADVENTURE);

        p.removePotionEffect(PotionEffectType.JUMP);
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.setWalkSpeed(0.2f);
    }

}
