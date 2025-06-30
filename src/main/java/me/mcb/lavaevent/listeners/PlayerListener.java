package me.mcb.lavaevent.listeners;

import me.mcb.lavaevent.MCBLavaEventPlugin;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;

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
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Only handle deaths during active events
        if (!plugin.getGameManager().isEventActive()) {
            return;
        }
        
        // Only handle deaths of alive players
        if (!plugin.getGameManager().isPlayerAlive(player.getUniqueId())) {
            return;
        }
        
        // Check if death was caused by lava
        if (isLavaRelatedDeath(event)) {
            // Play death effects
            playDeathEffects(player);
            
            // Eliminate the player
            plugin.getGameManager().eliminatePlayer(player);
            
            // Cancel the death (prevent item dropping and respawn)
            event.setCancelled(true);
            
            // Set player to full health and put in spectator mode
            player.setHealth(player.getMaxHealth());
            player.setGameMode(GameMode.SPECTATOR);
        }
    }
    
    private boolean isLavaRelatedDeath(PlayerDeathEvent event) {
        // Check death message or damage cause
        String deathMessage = event.getDeathMessage();
        if (deathMessage != null) {
            return deathMessage.contains("lava") || deathMessage.contains("tried to swim in lava");
        }
        
        // Check if player was in lava when they died
        Player player = event.getEntity();
        Location loc = player.getLocation();
        return loc.getBlock().getType() == Material.LAVA || 
               player.getFireTicks() > 0 ||
               loc.getY() <= plugin.getGameManager().getCurrentLavaLevel();
    }
    
    private void playDeathEffects(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        
        // Lightning effect
        if (plugin.getConfigManager().getConfig().getBoolean("effects.death-lightning", true)) {
            world.strikeLightningEffect(loc);
        }
        
        // Sound effect for all players
        String soundName = plugin.getConfigManager().getConfig().getString("effects.death-sound", "ENTITY_LIGHTNING_BOLT_THUNDER");
        try {
            Sound sound = Sound.valueOf(soundName);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getWorld().equals(world)) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0f, 1.0f);
                }
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid death sound: " + soundName);
        }
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
    
    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        
        // Only track water bucket usage during active events
        if (!plugin.getGameManager().isEventActive()) {
            return;
        }
        
        // Only track water buckets, not lava buckets
        if (event.getBucket() != Material.WATER_BUCKET) {
            return;
        }
        
        // Check if player can use water bucket
        if (!plugin.getGameManager().canUseWaterBucket(player.getUniqueId())) {
            event.setCancelled(true);
            
            // Send appropriate message
            if (plugin.getConfigManager().areWaterBucketsDisabled()) {
                plugin.getMessageUtils().sendMessage(player, "player.water-bucket.disabled");
            } else {
                plugin.getMessageUtils().sendMessage(player, "player.water-bucket.limit-reached");
            }
            return;
        }
        
        // Record the water bucket usage
        plugin.getGameManager().recordWaterBucketUse(player.getUniqueId());
    }
} 