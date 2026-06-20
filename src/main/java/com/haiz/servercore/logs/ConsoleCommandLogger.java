package com.haiz.servercore.logs;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.discord.DiscordEmbedFactory;
import com.haiz.servercore.discord.DiscordLogService;
import com.haiz.servercore.metrics.MetricsManager;
import com.haiz.servercore.storage.DatabaseManager;
import com.haiz.servercore.utils.TextUtils;
import com.haiz.servercore.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

public final class ConsoleCommandLogger implements Listener {
    private final HaizServerCore plugin;
    private final DatabaseManager database;
    private final MetricsManager metrics;
    private final DiscordLogService discord;

    public ConsoleCommandLogger(HaizServerCore plugin, DatabaseManager database, MetricsManager metrics, DiscordLogService discord) {
        this.plugin = plugin;
        this.database = database;
        this.metrics = metrics;
        this.discord = discord;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onConsoleCommand(ServerCommandEvent event) {
        if (!plugin.config().isConsoleCommandLogEnabled()) {
            return;
        }
        String sanitized = TextUtils.sanitizeCommand(event.getCommand(), plugin.config());
        metrics.recordConsoleCommand();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> database.players().logCommand(event.getSender().getName(), "CONSOLE", sanitized, null, TimeUtils.nowSeconds()));
        discord.commandLog(DiscordEmbedFactory.consoleCommand(event.getSender().getName(), sanitized));
        if (plugin.config().isDangerousConsoleCommand(TextUtils.commandRoot(sanitized))) {
            discord.alert(DiscordEmbedFactory.alert("Comando perigoso no console", "`" + sanitized + "` foi executado por " + event.getSender().getName()));
        }
    }
}
