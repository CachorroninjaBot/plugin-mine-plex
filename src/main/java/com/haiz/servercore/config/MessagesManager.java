package com.haiz.servercore.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MessagesManager {
    private final JavaPlugin plugin;
    private FileConfiguration messages;

    public MessagesManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key) {
        String prefix = messages.getString("prefix", "");
        String value = messages.getString(key, key);
        return color(prefix + value);
    }

    public String raw(String key) {
        return color(messages.getString(key, key));
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }
}
