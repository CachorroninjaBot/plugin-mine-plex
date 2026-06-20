package com.haiz.servercore.logs;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.discord.DiscordLogService;
import com.haiz.servercore.metrics.MetricsManager;
import com.haiz.servercore.storage.DatabaseManager;
import org.bukkit.event.HandlerList;

public final class LogManager {
    private final HaizServerCore plugin;
    private final MinecraftEventLogger minecraftEventLogger;
    private final ConsoleCommandLogger consoleCommandLogger;

    public LogManager(HaizServerCore plugin, DatabaseManager database, MetricsManager metrics, DiscordLogService discordLogService) {
        this.plugin = plugin;
        this.minecraftEventLogger = new MinecraftEventLogger(plugin, database, metrics, discordLogService);
        this.consoleCommandLogger = new ConsoleCommandLogger(plugin, database, metrics, discordLogService);
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(minecraftEventLogger, plugin);
        plugin.getServer().getPluginManager().registerEvents(consoleCommandLogger, plugin);
    }

    public void unregister() {
        HandlerList.unregisterAll(minecraftEventLogger);
        HandlerList.unregisterAll(consoleCommandLogger);
    }

    public void reload() {
        unregister();
        register();
    }
}
