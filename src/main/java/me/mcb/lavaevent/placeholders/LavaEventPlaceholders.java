package me.mcb.lavaevent.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.mcb.lavaevent.MCBLavaEventPlugin;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class LavaEventPlaceholders extends PlaceholderExpansion {
    
    private final MCBLavaEventPlugin plugin;
    
    public LavaEventPlaceholders(MCBLavaEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "lavaevent";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params.toLowerCase()) {
            case "active":
                return plugin.getGameManager().isEventActive() ? "true" : "false";
                
            case "grace_period":
                return plugin.getGameManager().isGracePeriodActive() ? "true" : "false";
                
            case "lava_level":
                return String.valueOf((int) plugin.getGameManager().getCurrentLavaLevel());
                
            case "alive_count":
                return String.valueOf(plugin.getGameManager().getAlivePlayerCount());
                
            case "is_alive":
                if (player == null) return "false";
                return plugin.getGameManager().isPlayerAlive(player.getUniqueId()) ? "true" : "false";
                
            case "is_spectator":
                if (player == null) return "false";
                return plugin.getGameManager().isPlayerSpectator(player.getUniqueId()) ? "true" : "false";
                
            case "world":
                return plugin.getGameManager().getEventWorld() != null ? 
                    plugin.getGameManager().getEventWorld().getName() : "none";
                
            case "status":
                if (!plugin.getGameManager().isEventActive()) {
                    return "inactive";
                } else if (plugin.getGameManager().isGracePeriodActive()) {
                    return "grace_period";
                } else {
                    return "active";
                }
                
            case "waterbucket_used":
                if (player == null) return "0";
                return String.valueOf(plugin.getGameManager().getWaterBucketUsage(player.getUniqueId()));
                
            case "waterbucket_remaining":
                if (player == null) return "0";
                int remaining = plugin.getGameManager().getRemainingWaterBuckets(player.getUniqueId());
                return remaining == -1 ? "unlimited" : String.valueOf(remaining);
                
            case "waterbucket_max":
                int maxUses = plugin.getConfigManager().getMaxWaterBucketsPerPlayer();
                return maxUses == -1 ? "unlimited" : String.valueOf(maxUses);
                
            case "waterbucket_disabled":
                return plugin.getConfigManager().areWaterBucketsDisabled() ? "true" : "false";
                
            default:
                return null;
        }
    }
} 