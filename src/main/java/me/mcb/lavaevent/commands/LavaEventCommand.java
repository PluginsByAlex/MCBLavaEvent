package me.mcb.lavaevent.commands;

import me.mcb.lavaevent.MCBLavaEventPlugin;
import me.mcb.lavaevent.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LavaEventCommand implements CommandExecutor, TabCompleter {
    
    private final MCBLavaEventPlugin plugin;
    private final MessageUtils messageUtils;
    
    public LavaEventCommand(MCBLavaEventPlugin plugin) {
        this.plugin = plugin;
        this.messageUtils = plugin.getMessageUtils();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("waterbuckets") || args[0].equalsIgnoreCase("wb"))) {
            // Allow players to check their own water bucket usage
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (args.length == 1) {
                    // Show player's own usage
                    if (!plugin.getGameManager().isEventActive()) {
                        messageUtils.sendMessage(sender, "commands.waterbuckets.no-event");
                        return true;
                    }
                    showPlayerWaterBucketUsage(sender, player);
                    return true;
                }
            }
        }
        
        if (!sender.hasPermission("lavaevent.admin")) {
            messageUtils.sendMessage(sender, "commands.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                handleStartCommand(sender);
                break;
            case "stop":
                handleStopCommand(sender);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "setup":
                handleSetupCommand(sender, args);
                break;
            case "waterbuckets":
            case "wb":
                handleWaterBucketsCommand(sender, args);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }
        
        return true;
    }
    
    private void handleStartCommand(CommandSender sender) {
        if (plugin.getGameManager().isEventActive()) {
            messageUtils.sendMessage(sender, "commands.start.already-running");
            return;
        }
        
        String worldName = plugin.getConfigManager().getEventWorld();
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            Map<String, String> placeholders = MessageUtils.createPlaceholders("world", worldName);
            messageUtils.sendMessage(sender, "commands.start.no-world", placeholders);
            return;
        }
        
        boolean success = plugin.getGameManager().startEvent();
        if (success) {
            messageUtils.sendMessage(sender, "commands.start.success");
        } else {
            messageUtils.sendMessage(sender, "errors.command-error");
        }
    }
    
    private void handleStopCommand(CommandSender sender) {
        if (!plugin.getGameManager().isEventActive()) {
            messageUtils.sendMessage(sender, "commands.stop.not-running");
            return;
        }
        
        plugin.getGameManager().stopEvent();
        messageUtils.sendMessage(sender, "commands.stop.success");
    }
    
    private void handleReloadCommand(CommandSender sender) {
        try {
            plugin.getConfigManager().reloadConfigs();
            messageUtils.sendMessage(sender, "commands.reload-success");
        } catch (Exception e) {
            plugin.getLogger().severe("Error reloading configs: " + e.getMessage());
            messageUtils.sendMessage(sender, "errors.command-error");
        }
    }
    
    private void handleSetupCommand(CommandSender sender, String[] args) {
        if (args.length != 4) {
            messageUtils.sendMessage(sender, "commands.setup.usage");
            return;
        }
        
        try {
            int centerX = Integer.parseInt(args[1]);
            int centerZ = Integer.parseInt(args[2]);
            int radius = Integer.parseInt(args[3]);
            
            String worldName = plugin.getConfigManager().getEventWorld();
            World world = Bukkit.getWorld(worldName);
            
            if (world == null) {
                Map<String, String> placeholders = MessageUtils.createPlaceholders("world", worldName);
                messageUtils.sendMessage(sender, "errors.world-not-found", placeholders);
                return;
            }
            
            // Setup world border
            WorldBorder border = world.getWorldBorder();
            border.setCenter(centerX, centerZ);
            border.setSize(radius * 2);
            
            // Update config
            plugin.getConfigManager().getConfig().set("game.border.center-x", centerX);
            plugin.getConfigManager().getConfig().set("game.border.center-z", centerZ);
            plugin.getConfigManager().getConfig().set("game.border.starting-size", radius * 2);
            plugin.getConfigManager().saveConfig();
            
            messageUtils.sendMessage(sender, "commands.setup.success");
            
        } catch (NumberFormatException e) {
            Map<String, String> placeholders = MessageUtils.createPlaceholders("input", String.join(" ", args));
            messageUtils.sendMessage(sender, "errors.invalid-number", placeholders);
        }
    }
    
    private void handleWaterBucketsCommand(CommandSender sender, String[] args) {
        if (!plugin.getGameManager().isEventActive()) {
            messageUtils.sendMessage(sender, "commands.waterbuckets.no-event");
            return;
        }
        
        if (args.length == 1) {
            // Show overall water bucket status
            showWaterBucketStatus(sender);
        } else if (args.length == 2) {
            // Show specific player's water bucket usage
            String playerName = args[1];
            Player target = Bukkit.getPlayer(playerName);
            
            if (target == null) {
                Map<String, String> placeholders = MessageUtils.createPlaceholders("player", playerName);
                messageUtils.sendMessage(sender, "errors.player-not-found", placeholders);
                return;
            }
            
            showPlayerWaterBucketUsage(sender, target);
        } else {
            messageUtils.sendMessage(sender, "commands.waterbuckets.usage");
        }
    }
    
    private void showWaterBucketStatus(CommandSender sender) {
        int maxUses = plugin.getConfigManager().getMaxWaterBucketsPerPlayer();
        boolean disabled = plugin.getConfigManager().areWaterBucketsDisabled();
        
        sender.sendMessage("§6§l=== Water Bucket Status ===");
        
        if (disabled) {
            sender.sendMessage("§c§lWater buckets are DISABLED during this event!");
            return;
        }
        
        if (maxUses == -1) {
            sender.sendMessage("§a§lUnlimited water bucket usage allowed");
        } else {
            sender.sendMessage("§e§lMax water buckets per player: §f" + maxUses);
        }
        
        sender.sendMessage("");
        sender.sendMessage("§6Current Player Usage:");
        
        boolean hasUsage = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getGameManager().isPlayerAlive(player.getUniqueId()) || 
                plugin.getGameManager().isPlayerSpectator(player.getUniqueId())) {
                
                int usage = plugin.getGameManager().getWaterBucketUsage(player.getUniqueId());
                if (usage > 0 || maxUses != -1) {
                    hasUsage = true;
                    int remaining = plugin.getGameManager().getRemainingWaterBuckets(player.getUniqueId());
                    String remainingText = remaining == -1 ? "∞" : String.valueOf(remaining);
                    
                    sender.sendMessage("§7- §b" + player.getName() + "§7: §f" + usage + 
                                     (maxUses == -1 ? "" : "/" + maxUses) + 
                                     " §7(§a" + remainingText + " remaining§7)");
                }
            }
        }
        
        if (!hasUsage) {
            sender.sendMessage("§7No water bucket usage recorded yet.");
        }
    }
    
    private void showPlayerWaterBucketUsage(CommandSender sender, Player target) {
        int usage = plugin.getGameManager().getWaterBucketUsage(target.getUniqueId());
        int remaining = plugin.getGameManager().getRemainingWaterBuckets(target.getUniqueId());
        int maxUses = plugin.getConfigManager().getMaxWaterBucketsPerPlayer();
        
        sender.sendMessage("§6§l=== " + target.getName() + "'s Water Bucket Usage ===");
        
        if (plugin.getConfigManager().areWaterBucketsDisabled()) {
            sender.sendMessage("§c§lWater buckets are DISABLED during this event!");
            return;
        }
        
        sender.sendMessage("§7Used: §f" + usage + (maxUses == -1 ? "" : "/" + maxUses));
        
        if (remaining == -1) {
            sender.sendMessage("§7Remaining: §a∞ (Unlimited)");
        } else {
            sender.sendMessage("§7Remaining: §a" + remaining);
            
            if (remaining == 0) {
                sender.sendMessage("§c§lThis player has reached their limit!");
            }
        }
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6§l=== MCBLavaEvent Commands ===");
        
        if (sender.hasPermission("lavaevent.admin")) {
            sender.sendMessage("§e/lavaevent start §7- Start a lava event");
            sender.sendMessage("§e/lavaevent stop §7- Stop the current event");
            sender.sendMessage("§e/lavaevent reload §7- Reload configuration files");
            sender.sendMessage("§e/lavaevent setup <x> <z> <radius> §7- Setup event area");
            sender.sendMessage("§e/lavaevent waterbuckets [player] §7- Check water bucket usage status");
        } else {
            sender.sendMessage("§e/lavaevent waterbuckets §7- Check your water bucket usage");
        }
        
        if (plugin.getGameManager().isEventActive()) {
            sender.sendMessage("");
            sender.sendMessage("§6Current Event Status:");
            sender.sendMessage("§7- Active: §aYes");
            sender.sendMessage("§7- Lava Level: §c" + (int) plugin.getGameManager().getCurrentLavaLevel());
            sender.sendMessage("§7- Players Alive: §b" + plugin.getGameManager().getAlivePlayerCount());
            sender.sendMessage("§7- Grace Period: " + (plugin.getGameManager().isGracePeriodActive() ? "§aActive" : "§cInactive"));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lavaevent.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            List<String> completions = Arrays.asList("start", "stop", "reload", "setup", "waterbuckets");
            return filterCompletions(completions, args[0]);
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                return Arrays.asList(String.valueOf((int) player.getLocation().getX()));
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("setup")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                return Arrays.asList(String.valueOf((int) player.getLocation().getZ()));
            }
        }
        
        if (args.length == 4 && args[0].equalsIgnoreCase("setup")) {
            return Arrays.asList("100", "200", "500", "1000");
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("waterbuckets") || args[0].equalsIgnoreCase("wb"))) {
            // Tab complete player names for water bucket command
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getGameManager().isPlayerAlive(player.getUniqueId()) || 
                    plugin.getGameManager().isPlayerSpectator(player.getUniqueId())) {
                    playerNames.add(player.getName());
                }
            }
            return filterCompletions(playerNames, args[1]);
        }
        
        return new ArrayList<>();
    }
    
    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(completion);
            }
        }
        return filtered;
    }
} 