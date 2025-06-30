package me.mcb.lavaevent.utils;

import me.mcb.lavaevent.MCBLavaEventPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class MessageUtils {
    
    private final MCBLavaEventPlugin plugin;
    private final MiniMessage miniMessage;
    
    public MessageUtils(MCBLavaEventPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    public String getMessage(String path) {
        return plugin.getConfigManager().getMessages().getString(path, "Message not found: " + path);
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
    
    public Component getComponent(String path) {
        return miniMessage.deserialize(getMessage(path));
    }
    
    public Component getComponent(String path, Map<String, String> placeholders) {
        return miniMessage.deserialize(getMessage(path, placeholders));
    }
    
    public void sendMessage(CommandSender sender, String path) {
        String prefix = getMessage("prefix");
        String message = getMessage(path);
        Component component = miniMessage.deserialize(prefix + message);
        sender.sendMessage(component);
    }
    
    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String prefix = getMessage("prefix");
        String message = getMessage(path, placeholders);
        Component component = miniMessage.deserialize(prefix + message);
        sender.sendMessage(component);
    }
    
    public void sendRawMessage(CommandSender sender, String path) {
        Component component = getComponent(path);
        sender.sendMessage(component);
    }
    
    public void sendRawMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        Component component = getComponent(path, placeholders);
        sender.sendMessage(component);
    }
    
    public void broadcast(String path) {
        String prefix = getMessage("prefix");
        String message = getMessage(path);
        Component component = miniMessage.deserialize(prefix + message);
        Bukkit.broadcast(component);
    }
    
    public void broadcast(String path, Map<String, String> placeholders) {
        String prefix = getMessage("prefix");
        String message = getMessage(path, placeholders);
        Component component = miniMessage.deserialize(prefix + message);
        Bukkit.broadcast(component);
    }
    
    public void broadcastRaw(String path) {
        Component component = getComponent(path);
        Bukkit.broadcast(component);
    }
    
    public void broadcastRaw(String path, Map<String, String> placeholders) {
        Component component = getComponent(path, placeholders);
        Bukkit.broadcast(component);
    }
    
    public void sendTitle(Player player, String titlePath, String subtitlePath) {
        Component titleComponent = getComponent(titlePath);
        Component subtitleComponent = getComponent(subtitlePath);
        
        Title title = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                Duration.ofMillis(500),  // fade in
                Duration.ofSeconds(3),   // stay
                Duration.ofMillis(500)   // fade out
            )
        );
        
        player.showTitle(title);
    }
    
    public void sendTitle(Player player, String titlePath, String subtitlePath, Map<String, String> placeholders) {
        Component titleComponent = getComponent(titlePath, placeholders);
        Component subtitleComponent = getComponent(subtitlePath, placeholders);
        
        Title title = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                Duration.ofMillis(500),  // fade in
                Duration.ofSeconds(3),   // stay
                Duration.ofMillis(500)   // fade out
            )
        );
        
        player.showTitle(title);
    }
    
    public void broadcastTitle(String titlePath, String subtitlePath) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendTitle(player, titlePath, subtitlePath);
        }
    }
    
    public void broadcastTitle(String titlePath, String subtitlePath, Map<String, String> placeholders) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendTitle(player, titlePath, subtitlePath, placeholders);
        }
    }
    
    // Convenience method for creating placeholder maps
    public static Map<String, String> createPlaceholders(String... pairs) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i + 1 < pairs.length) {
                placeholders.put(pairs[i], pairs[i + 1]);
            }
        }
        return placeholders;
    }
} 