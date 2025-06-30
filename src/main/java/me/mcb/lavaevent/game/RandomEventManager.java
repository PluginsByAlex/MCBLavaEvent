package me.mcb.lavaevent.game;

import me.mcb.lavaevent.MCBLavaEventPlugin;
import me.mcb.lavaevent.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RandomEventManager {
    
    private final MCBLavaEventPlugin plugin;
    private final MessageUtils messageUtils;
    private final Random random;
    
    public RandomEventManager(MCBLavaEventPlugin plugin) {
        this.plugin = plugin;
        this.messageUtils = plugin.getMessageUtils();
        this.random = new Random();
    }
    
    public void triggerRandomEvent(World world, List<UUID> alivePlayers) {
        if (alivePlayers.isEmpty()) {
            return;
        }
        
        double chance = plugin.getConfigManager().getConfig().getDouble("game.random-events.chance", 0.3);
        if (random.nextDouble() > chance) {
            return;
        }
        
        // Choose random event
        RandomEventType eventType = getRandomEventType();
        
        switch (eventType) {
            case TNT_RAIN:
                startTNTRain(world, alivePlayers);
                break;
            case MOB_SPAWN:
                spawnHostileMobs(world, alivePlayers);
                break;
            case LIGHTNING_STORM:
                startLightningStorm(world, alivePlayers);
                break;
        }
    }
    
    private RandomEventType getRandomEventType() {
        RandomEventType[] events = RandomEventType.values();
        RandomEventType selectedEvent;
        
        do {
            selectedEvent = events[random.nextInt(events.length)];
        } while (!isEventEnabled(selectedEvent));
        
        return selectedEvent;
    }
    
    private boolean isEventEnabled(RandomEventType eventType) {
        String path = "game.random-events.events." + eventType.getConfigKey() + ".enabled";
        return plugin.getConfigManager().getConfig().getBoolean(path, true);
    }
    
    private void startTNTRain(World world, List<UUID> alivePlayers) {
        messageUtils.broadcastRaw("random-events.tnt-rain.start");
        messageUtils.broadcastRaw("random-events.tnt-rain.warning");
        
        int duration = plugin.getConfigManager().getConfig().getInt("game.random-events.events.tnt-rain.duration", 10);
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double borderSize = border.getSize();
        int radius = (int) (borderSize / 2);
        
        new BukkitRunnable() {
            int timeLeft = duration;
            
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    cancel();
                    return;
                }
                
                // Spawn TNT at random locations
                for (int i = 0; i < 3; i++) {
                    double x = center.getX() + (random.nextDouble() - 0.5) * borderSize;
                    double z = center.getZ() + (random.nextDouble() - 0.5) * borderSize;
                    double y = world.getHighestBlockYAt((int) x, (int) z) + 50;
                    
                    Location tntLocation = new Location(world, x, y, z);
                    TNTPrimed tnt = world.spawn(tntLocation, TNTPrimed.class);
                    tnt.setFuseTicks(60); // 3 seconds fuse
                }
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void spawnHostileMobs(World world, List<UUID> alivePlayers) {
        messageUtils.broadcastRaw("random-events.mob-spawn.start");
        
        int mobCount = plugin.getConfigManager().getConfig().getInt("game.random-events.events.mob-spawn.count", 5);
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double borderSize = border.getSize();
        
        EntityType[] hostileMobs = {
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, 
            EntityType.SPIDER, EntityType.ENDERMAN, EntityType.WITCH
        };
        
        for (int i = 0; i < mobCount; i++) {
            double x = center.getX() + (random.nextDouble() - 0.5) * borderSize;
            double z = center.getZ() + (random.nextDouble() - 0.5) * borderSize;
            double y = world.getHighestBlockYAt((int) x, (int) z) + 1;
            
            Location spawnLocation = new Location(world, x, y, z);
            EntityType mobType = hostileMobs[random.nextInt(hostileMobs.length)];
            
            Entity mob = world.spawnEntity(spawnLocation, mobType);
            if (mob instanceof Monster) {
                ((Monster) mob).setTarget(getNearestPlayer(spawnLocation, alivePlayers));
            }
        }
    }
    
    private void startLightningStorm(World world, List<UUID> alivePlayers) {
        messageUtils.broadcastRaw("random-events.lightning-storm.start");
        messageUtils.broadcastRaw("random-events.lightning-storm.warning");
        
        int duration = plugin.getConfigManager().getConfig().getInt("game.random-events.events.lightning-storm.duration", 15);
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double borderSize = border.getSize();
        
        new BukkitRunnable() {
            int timeLeft = duration;
            
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    cancel();
                    return;
                }
                
                // Strike lightning at random locations
                for (int i = 0; i < 2; i++) {
                    double x = center.getX() + (random.nextDouble() - 0.5) * borderSize;
                    double z = center.getZ() + (random.nextDouble() - 0.5) * borderSize;
                    double y = world.getHighestBlockYAt((int) x, (int) z);
                    
                    Location strikeLocation = new Location(world, x, y, z);
                    world.strikeLightning(strikeLocation);
                }
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private Player getNearestPlayer(Location location, List<UUID> alivePlayers) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (UUID playerId : alivePlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.getWorld().equals(location.getWorld())) {
                double distance = player.getLocation().distance(location);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = player;
                }
            }
        }
        
        return nearest;
    }
    
    private enum RandomEventType {
        TNT_RAIN("tnt-rain"),
        MOB_SPAWN("mob-spawn"),
        LIGHTNING_STORM("lightning-storm");
        
        private final String configKey;
        
        RandomEventType(String configKey) {
            this.configKey = configKey;
        }
        
        public String getConfigKey() {
            return configKey;
        }
    }
} 