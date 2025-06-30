package me.mcb.lavaevent.game;

import me.mcb.lavaevent.MCBLavaEventPlugin;
import me.mcb.lavaevent.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager {
    
    private final MCBLavaEventPlugin plugin;
    private final MessageUtils messageUtils;
    
    private boolean eventActive = false;
    private boolean gracePeriodActive = false;
    private World eventWorld;
    private double currentLavaLevel;
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    
    private BukkitTask lavaRiseTask;
    private BukkitTask borderShrinkTask;
    private BukkitTask playerCheckTask;
    private BukkitTask randomEventTask;
    
    private RandomEventManager randomEventManager;
    
    public GameManager(MCBLavaEventPlugin plugin) {
        this.plugin = plugin;
        this.messageUtils = plugin.getMessageUtils();
        this.randomEventManager = new RandomEventManager(plugin);
    }
    
    public boolean startEvent() {
        if (eventActive) {
            return false;
        }
        
        String worldName = plugin.getConfigManager().getEventWorld();
        eventWorld = Bukkit.getWorld(worldName);
        
        if (eventWorld == null) {
            plugin.getLogger().severe("Event world '" + worldName + "' not found!");
            return false;
        }
        
        // Initialize event
        eventActive = true;
        gracePeriodActive = true;
        currentLavaLevel = plugin.getConfigManager().getConfig().getDouble("game.starting-lava-level", -64);
        
        // Add all online players to the event
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(eventWorld)) {
                alivePlayers.add(player.getUniqueId());
            }
        }
        
        // Start countdown
        startCountdown();
        
        return true;
    }
    
    private void startCountdown() {
        int delay = plugin.getConfigManager().getStartDelay();
        
        new BukkitRunnable() {
            int timeLeft = delay;
            
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    startMainEvent();
                    cancel();
                    return;
                }
                
                if (timeLeft <= 5 || timeLeft % 10 == 0) {
                    Map<String, String> placeholders = MessageUtils.createPlaceholders("time", String.valueOf(timeLeft));
                    messageUtils.broadcast("event.starting", placeholders);
                    messageUtils.broadcastTitle("event.countdown.title", "event.countdown.subtitle", placeholders);
                }
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void startMainEvent() {
        messageUtils.broadcastRaw("event.started");
        
        // Start grace period
        startGracePeriod();
        
        // Setup world border
        setupWorldBorder();
        
        // Start lava rising
        startLavaRising();
        
        // Start player checking
        startPlayerChecking();
        
        // Start random events if enabled
        if (plugin.getConfigManager().isRandomEventsEnabled()) {
            startRandomEvents();
        }
    }
    
    private void startGracePeriod() {
        int gracePeriod = plugin.getConfigManager().getGracePeriod();
        
        Map<String, String> placeholders = MessageUtils.createPlaceholders("time", String.valueOf(gracePeriod));
        messageUtils.broadcast("event.grace-period.start", placeholders);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                gracePeriodActive = false;
                messageUtils.broadcastRaw("event.grace-period.end");
            }
        }.runTaskLater(plugin, gracePeriod * 20L);
    }
    
    private void setupWorldBorder() {
        WorldBorder border = eventWorld.getWorldBorder();
        
        int startingSize = plugin.getConfigManager().getConfig().getInt("game.border.starting-size", 1000);
        int finalSize = plugin.getConfigManager().getConfig().getInt("game.border.final-size", 50);
        int centerX = plugin.getConfigManager().getConfig().getInt("game.border.center-x", 0);
        int centerZ = plugin.getConfigManager().getConfig().getInt("game.border.center-z", 0);
        
        border.setCenter(centerX, centerZ);
        border.setSize(startingSize);
        
        // Start border shrinking
        borderShrinkTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventActive) {
                    cancel();
                    return;
                }
                
                double currentSize = border.getSize();
                double shrinkSpeed = plugin.getConfigManager().getBorderShrinkSpeed();
                
                if (currentSize > finalSize) {
                    border.setSize(Math.max(finalSize, currentSize - shrinkSpeed));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void startLavaRising() {
        lavaRiseTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventActive) {
                    cancel();
                    return;
                }
                
                double riseSpeed = plugin.getConfigManager().getLavaRiseSpeed();
                double maxHeight = plugin.getConfigManager().getMaxLavaHeight();
                
                if (currentLavaLevel < maxHeight) {
                    currentLavaLevel += riseSpeed;
                    placeLavaBlocks();
                    
                    // Broadcast lava level every 10 blocks
                    if ((int) currentLavaLevel % 10 == 0) {
                        Map<String, String> placeholders = MessageUtils.createPlaceholders("level", String.valueOf((int) currentLavaLevel));
                        messageUtils.broadcast("player.status.lava-level", placeholders);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void placeLavaBlocks() {
        WorldBorder border = eventWorld.getWorldBorder();
        Location center = border.getCenter();
        double borderSize = border.getSize();
        int radius = (int) (borderSize / 2);
        
        int lavaY = (int) Math.floor(currentLavaLevel);
        
        for (int x = (int) center.getX() - radius; x <= (int) center.getX() + radius; x++) {
            for (int z = (int) center.getZ() - radius; z <= (int) center.getZ() + radius; z++) {
                // Check if within circular border
                double distance = Math.sqrt(Math.pow(x - center.getX(), 2) + Math.pow(z - center.getZ(), 2));
                if (distance <= radius) {
                    Block block = eventWorld.getBlockAt(x, lavaY, z);
                    if (block.getType() == Material.AIR || block.getType() == Material.WATER) {
                        block.setType(Material.LAVA);
                    }
                }
            }
        }
    }
    
    private void startPlayerChecking() {
        playerCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventActive) {
                    cancel();
                    return;
                }
                
                Iterator<UUID> iterator = alivePlayers.iterator();
                while (iterator.hasNext()) {
                    UUID playerId = iterator.next();
                    Player player = Bukkit.getPlayer(playerId);
                    
                    if (player == null || !player.isOnline()) {
                        iterator.remove();
                        continue;
                    }
                    
                    // Check if player is in lava or below lava level
                    Location loc = player.getLocation();
                    if (loc.getY() <= currentLavaLevel || 
                        loc.getBlock().getType() == Material.LAVA ||
                        player.getFireTicks() > 0) {
                        
                        // Check for bypass permission
                        if (!player.hasPermission("lavaevent.bypass")) {
                            eliminatePlayer(player);
                            iterator.remove();
                        }
                    }
                }
                
                // Check win condition
                if (alivePlayers.size() <= 1) {
                    endEvent();
                }
                
                // Broadcast alive count every 30 seconds
                if (System.currentTimeMillis() % 30000 < 1000) {
                    Map<String, String> placeholders = MessageUtils.createPlaceholders("count", String.valueOf(alivePlayers.size()));
                    messageUtils.broadcast("player.status.alive", placeholders);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Check every 0.5 seconds
    }
    
    private void startRandomEvents() {
        int interval = plugin.getConfigManager().getConfig().getInt("game.random-events.interval", 120);
        
        randomEventTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventActive) {
                    cancel();
                    return;
                }
                
                randomEventManager.triggerRandomEvent(eventWorld, new ArrayList<>(alivePlayers));
            }
        }.runTaskTimer(plugin, interval * 20L, interval * 20L);
    }
    
    public void eliminatePlayer(Player player) {
        if (!alivePlayers.contains(player.getUniqueId())) {
            return;
        }
        
        alivePlayers.remove(player.getUniqueId());
        spectators.add(player.getUniqueId());
        
        // Set to spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        
        // Teleport to safe location
        if (plugin.getConfigManager().getConfig().getBoolean("spectator.safe-teleport", true)) {
            int spectatorHeight = plugin.getConfigManager().getConfig().getInt("spectator.spectator-height", 250);
            Location safeLocation = player.getLocation().clone();
            safeLocation.setY(spectatorHeight);
            player.teleport(safeLocation);
        }
        
        // Broadcast elimination
        Map<String, String> placeholders = MessageUtils.createPlaceholders("player", player.getName());
        messageUtils.broadcastRaw("player.eliminated", placeholders);
        messageUtils.sendMessage(player, "player.spectator-mode");
    }
    
    public void addPlayerToEvent(Player player) {
        if (!eventActive) {
            return;
        }
        
        if (player.getWorld().equals(eventWorld)) {
            // Event is already running, make them a spectator
            spectators.add(player.getUniqueId());
            player.setGameMode(GameMode.SPECTATOR);
            messageUtils.sendMessage(player, "player.joined-late");
        }
    }
    
    public void stopEvent() {
        if (!eventActive) {
            return;
        }
        
        eventActive = false;
        gracePeriodActive = false;
        
        // Cancel all tasks
        if (lavaRiseTask != null) lavaRiseTask.cancel();
        if (borderShrinkTask != null) borderShrinkTask.cancel();
        if (playerCheckTask != null) playerCheckTask.cancel();
        if (randomEventTask != null) randomEventTask.cancel();
        
        // Reset players
        for (UUID playerId : alivePlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        
        for (UUID playerId : spectators) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        
        // Clear collections
        alivePlayers.clear();
        spectators.clear();
        
        messageUtils.broadcastRaw("event.stopped");
    }
    
    private void endEvent() {
        if (alivePlayers.size() == 1) {
            UUID winnerId = alivePlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null) {
                Map<String, String> placeholders = MessageUtils.createPlaceholders("player", winner.getName());
                messageUtils.broadcastRaw("event.won", placeholders);
            }
        } else {
            messageUtils.broadcastRaw("event.no-winner");
        }
        
        stopEvent();
    }
    
    // Getters
    public boolean isEventActive() {
        return eventActive;
    }
    
    public boolean isGracePeriodActive() {
        return gracePeriodActive;
    }
    
    public double getCurrentLavaLevel() {
        return currentLavaLevel;
    }
    
    public int getAlivePlayerCount() {
        return alivePlayers.size();
    }
    
    public boolean isPlayerAlive(UUID playerId) {
        return alivePlayers.contains(playerId);
    }
    
    public boolean isPlayerSpectator(UUID playerId) {
        return spectators.contains(playerId);
    }
    
    public World getEventWorld() {
        return eventWorld;
    }
} 