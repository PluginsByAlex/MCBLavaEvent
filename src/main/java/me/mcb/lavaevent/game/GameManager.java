package me.mcb.lavaevent.game;

import me.mcb.lavaevent.MCBLavaEventPlugin;
import me.mcb.lavaevent.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    
    private final MCBLavaEventPlugin plugin;
    private final MessageUtils messageUtils;
    
    private boolean eventActive = false;
    private boolean gracePeriodActive = false;
    private World eventWorld;
    private double currentLavaLevel;
    private final Set<UUID> alivePlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> spectators = ConcurrentHashMap.newKeySet();
    
    // Water bucket usage tracking
    private final Map<UUID, Integer> waterBucketUsage = new ConcurrentHashMap<>();
    
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
            private int lastBroadcastLevel = -999;
            
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
                    
                    // Broadcast lava level at configured intervals
                    int broadcastInterval = plugin.getConfigManager().getLavaLevelBroadcastInterval();
                    int currentLevel = (int) Math.floor(currentLavaLevel);
                    
                    if (currentLevel >= lastBroadcastLevel + broadcastInterval) {
                        lastBroadcastLevel = currentLevel;
                        Map<String, String> placeholders = MessageUtils.createPlaceholders("level", String.valueOf(currentLevel));
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
        int halfSize = (int) (borderSize / 2);
        
        int lavaY = (int) Math.floor(currentLavaLevel);
        
        // Place lava in a square pattern covering the entire world border area
        int minX = (int) center.getX() - halfSize;
        int maxX = (int) center.getX() + halfSize;
        int minZ = (int) center.getZ() - halfSize;
        int maxZ = (int) center.getZ() + halfSize;
        
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = eventWorld.getBlockAt(x, lavaY, z);
                if (block.getType() == Material.AIR || block.getType() == Material.WATER) {
                    block.setType(Material.LAVA);
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
                
                // Create a copy of the set to avoid concurrent modification
                Set<UUID> playersToCheck = new HashSet<>(alivePlayers);
                
                for (UUID playerId : playersToCheck) {
                    Player player = Bukkit.getPlayer(playerId);
                    
                    if (player == null || !player.isOnline()) {
                        // Safely remove disconnected players
                        alivePlayers.remove(playerId);
                        continue;
                    }
                    
                    // Only check for disconnected players or those with bypass permission
                    // Death handling is now done in PlayerListener
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
        }.runTaskTimer(plugin, 0L, 20L); // Check every second
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
        clearWaterBucketUsage();
        
        messageUtils.broadcastRaw("event.stopped");
    }
    
    private void endEvent() {
        if (alivePlayers.size() == 1) {
            UUID winnerId = alivePlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null) {
                // Play win effects
                playWinEffects(winner);
                
                Map<String, String> placeholders = MessageUtils.createPlaceholders("player", winner.getName());
                messageUtils.broadcastRaw("event.won", placeholders);
            }
        } else {
            messageUtils.broadcastRaw("event.no-winner");
        }
        
        stopEvent();
    }
    
    private void playWinEffects(Player winner) {
        Location loc = winner.getLocation();
        World world = loc.getWorld();
        
        // Fireworks effect
        if (plugin.getConfigManager().getConfig().getBoolean("effects.win-fireworks", true)) {
            for (int i = 0; i < 5; i++) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        spawnFirework(loc.clone().add(
                            (Math.random() - 0.5) * 10,
                            Math.random() * 10,
                            (Math.random() - 0.5) * 10
                        ));
                    }
                }.runTaskLater(plugin, i * 10L);
            }
        }
        
        // Win sound for all players
        String soundName = plugin.getConfigManager().getConfig().getString("effects.win-sound", "UI_TOAST_CHALLENGE_COMPLETE");
        try {
            Sound sound = Sound.valueOf(soundName);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getWorld().equals(world)) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0f, 1.0f);
                }
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid win sound: " + soundName);
        }
        
        // Particles around winner
        world.spawnParticle(Particle.FIREWORK, loc, 50, 2, 2, 2, 0.1);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 30, 1, 1, 1, 0.1);
    }
    
    private void spawnFirework(Location location) {
        org.bukkit.entity.Firework firework = location.getWorld().spawn(location, org.bukkit.entity.Firework.class);
        org.bukkit.inventory.meta.FireworkMeta meta = firework.getFireworkMeta();
        
        // Create random firework effect
        org.bukkit.FireworkEffect effect = org.bukkit.FireworkEffect.builder()
            .withColor(Color.YELLOW, Color.ORANGE, Color.RED)
            .withFade(Color.WHITE)
            .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
            .withFlicker()
            .withTrail()
            .build();
            
        meta.addEffect(effect);
        meta.setPower(1);
        firework.setFireworkMeta(meta);
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
    
    // Water bucket usage methods
    public boolean canUseWaterBucket(UUID playerId) {
        if (!eventActive) {
            return true; // Allow water bucket usage when event is not active
        }
        
        // Check if water buckets are disabled during the event
        if (plugin.getConfigManager().areWaterBucketsDisabled()) {
            return false;
        }
        
        int maxUses = plugin.getConfigManager().getMaxWaterBucketsPerPlayer();
        if (maxUses == -1) {
            return true; // Unlimited usage
        }
        
        int currentUsage = waterBucketUsage.getOrDefault(playerId, 0);
        return currentUsage < maxUses;
    }
    
    public void recordWaterBucketUse(UUID playerId) {
        if (!eventActive) {
            return; // Don't track usage when event is not active
        }
        
        int currentUsage = waterBucketUsage.getOrDefault(playerId, 0);
        waterBucketUsage.put(playerId, currentUsage + 1);
        
        // Show usage message if enabled
        if (plugin.getConfigManager().showWaterBucketUsageMessages()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                int maxUses = plugin.getConfigManager().getMaxWaterBucketsPerPlayer();
                int remaining = maxUses == -1 ? -1 : maxUses - (currentUsage + 1);
                
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("used", String.valueOf(currentUsage + 1));
                placeholders.put("max", maxUses == -1 ? "∞" : String.valueOf(maxUses));
                placeholders.put("remaining", remaining == -1 ? "∞" : String.valueOf(Math.max(0, remaining)));
                
                messageUtils.sendMessage(player, "player.water-bucket.used", placeholders);
                
                if (remaining == 0) {
                    messageUtils.sendMessage(player, "player.water-bucket.limit-reached");
                }
            }
        }
    }
    
    public int getWaterBucketUsage(UUID playerId) {
        return waterBucketUsage.getOrDefault(playerId, 0);
    }
    
    public int getRemainingWaterBuckets(UUID playerId) {
        int maxUses = plugin.getConfigManager().getMaxWaterBucketsPerPlayer();
        if (maxUses == -1) {
            return -1; // Unlimited
        }
        
        int currentUsage = waterBucketUsage.getOrDefault(playerId, 0);
        return Math.max(0, maxUses - currentUsage);
    }
    
    private void clearWaterBucketUsage() {
        waterBucketUsage.clear();
    }
} 