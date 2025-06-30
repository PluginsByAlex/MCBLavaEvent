package me.mcb.lavaevent.listeners;

import me.mcb.lavaevent.MCBLavaEventPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class PlayerListener implements Listener {
    
    private final MCBLavaEventPlugin plugin;
    
    public PlayerListener(MCBLavaEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Add player to event if one is active
        plugin.getGameManager().addPlayerToEvent(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Player will be automatically removed from alive players list
        // during the next player check cycle in GameManager
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Prevent PvP during grace period
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        if (!plugin.getGameManager().isEventActive()) {
            return;
        }
        
        if (plugin.getGameManager().isGracePeriodActive()) {
            event.setCancelled(true);
        }
    }
} 