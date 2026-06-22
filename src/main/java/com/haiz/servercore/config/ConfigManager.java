package com.haiz.servercore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {
    private final JavaPlugin plugin;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public boolean isDiscordEnabled() {
        return plugin.getConfig().getBoolean("discord.enabled", true);
    }

    public String discordToken() {
        return plugin.getConfig().getString("discord.token", "");
    }

    public String guildId() {
        return plugin.getConfig().getString("discord.guild-id", "");
    }

    public boolean isActivityEnabled() {
        return plugin.getConfig().getBoolean("discord.activity.enabled", true);
    }

    public String activityText() {
        return plugin.getConfig().getString("discord.activity.text", "Monitorando o servidor");
    }
}
