package me.mcb.lavaevent.config;

import me.mcb.lavaevent.MCBLavaEventPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConfigManager {
    
    private final MCBLavaEventPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    
    private File configFile;
    private File messagesFile;
    
    public ConfigManager(MCBLavaEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        createConfigFiles();
        loadConfigFiles();
    }
    
    private void createConfigFiles() {
        // Create config.yml
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        // Create messages.yml
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }
    
    private void loadConfigFiles() {
        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load defaults
        config.setDefaults(YamlConfiguration.loadConfiguration(
            new InputStreamReader(plugin.getResource("config.yml"))));
        messages.setDefaults(YamlConfiguration.loadConfiguration(
            new InputStreamReader(plugin.getResource("messages.yml"))));
    }
    
    public void reloadConfigs() {
        loadConfigFiles();
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }
    
    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getMessages() {
        return messages;
    }
    
    // Convenience methods for common config values
    public int getStartDelay() {
        return config.getInt("game.start-delay", 30);
    }
    
    public double getLavaRiseSpeed() {
        return config.getDouble("game.lava-rise-speed", 0.5);
    }
    
    public int getMaxLavaHeight() {
        return config.getInt("game.max-lava-height", 200);
    }
    
    public double getBorderShrinkSpeed() {
        return config.getDouble("game.border-shrink-speed", 0.1);
    }
    
    public int getGracePeriod() {
        return config.getInt("game.grace-period", 60);
    }
    
    public boolean isRandomEventsEnabled() {
        return config.getBoolean("game.random-events.enabled", true);
    }
    
    public String getEventWorld() {
        return config.getString("game.world", "world");
    }
    
    public int getLavaLevelBroadcastInterval() {
        return config.getInt("game.lava-level-broadcast-interval", 10);
    }
    
    public int getMaxWaterBucketsPerPlayer() {
        return config.getInt("game.water-buckets.max-uses-per-player", 3);
    }
    
    public boolean areWaterBucketsDisabled() {
        return config.getBoolean("game.water-buckets.disabled", false);
    }
    
    public boolean showWaterBucketUsageMessages() {
        return config.getBoolean("game.water-buckets.show-usage-messages", true);
    }
} 