package me.mcb.lavaevent;

import me.mcb.lavaevent.commands.LavaEventCommand;
import me.mcb.lavaevent.config.ConfigManager;
import me.mcb.lavaevent.game.GameManager;
import me.mcb.lavaevent.listeners.PlayerListener;
import me.mcb.lavaevent.placeholders.LavaEventPlaceholders;
import me.mcb.lavaevent.utils.MessageUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class MCBLavaEventPlugin extends JavaPlugin {
    
    private static MCBLavaEventPlugin instance;
    private ConfigManager configManager;
    private GameManager gameManager;
    private MessageUtils messageUtils;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // Initialize message utils
        messageUtils = new MessageUtils(this);
        
        // Initialize game manager
        gameManager = new GameManager(this);
        
        // Register commands
        getCommand("lavaevent").setExecutor(new LavaEventCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LavaEventPlaceholders(this).register();
            getLogger().info("PlaceholderAPI integration enabled!");
        }
        
        getLogger().info("MCBLavaEvent has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isEventActive()) {
            gameManager.stopEvent();
        }
        getLogger().info("MCBLavaEvent has been disabled!");
    }
    
    public static MCBLavaEventPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public MessageUtils getMessageUtils() {
        return messageUtils;
    }
} 