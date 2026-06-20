package com.haiz.servercore.commands;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.metrics.PlayerStats;
import com.haiz.servercore.utils.NumberUtils;
import com.haiz.servercore.utils.TimeUtils;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class StatsCommand {
    private final HaizServerCore plugin;

    public StatsCommand(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public void serverSummary(CommandSender sender) {
        sender.sendMessage(plugin.messages().raw("metrics-header"));
        sender.sendMessage(plugin.metrics().reports().textSummary(plugin.metrics().snapshot()));
    }

    public void player(CommandSender sender, String name) {
        plugin.database().players().findByName(name).ifPresentOrElse(stats -> {
            sender.sendMessage("§dMetricas de " + stats.name());
            sender.sendMessage("§7Status: §f" + stats.status(TimeUtils.nowSeconds()));
            sender.sendMessage("§7Tempo total: §f" + TimeUtils.humanDuration(stats.totalPlaytimeSeconds()));
            sender.sendMessage("§7Sessoes: §f" + NumberUtils.integer(stats.totalSessions()));
            sender.sendMessage("§7Media por sessao: §f" + TimeUtils.humanDuration(stats.averageSessionSeconds()));
            sender.sendMessage("§7Comandos: §f" + NumberUtils.integer(stats.totalCommands()));
            sender.sendMessage("§7Chat: §f" + NumberUtils.integer(stats.totalChatMessages()));
            sender.sendMessage("§7Mortes/Kills: §f" + stats.totalDeaths() + "/" + stats.totalKills());
            sender.sendMessage("§7Blocos quebrados/colocados: §f" + stats.blocksBroken() + "/" + stats.blocksPlaced());
        }, () -> sender.sendMessage(plugin.messages().get("player-not-found")));
    }

    public void top(CommandSender sender, String type) {
        String column = switch (type.toLowerCase(java.util.Locale.ROOT)) {
            case "commands" -> "total_commands";
            case "deaths" -> "total_deaths";
            case "activity" -> "total_chat_messages";
            default -> "total_playtime_seconds";
        };
        List<PlayerStats> top = plugin.database().players().top(column, 10);
        sender.sendMessage("§dTop " + type);
        if (top.isEmpty()) {
            sender.sendMessage("§7Sem dados ainda.");
            return;
        }
        for (int i = 0; i < top.size(); i++) {
            PlayerStats stats = top.get(i);
            String value = switch (column) {
                case "total_commands" -> stats.totalCommands() + " comandos";
                case "total_deaths" -> stats.totalDeaths() + " mortes";
                case "total_chat_messages" -> stats.totalChatMessages() + " mensagens";
                default -> TimeUtils.humanDuration(stats.totalPlaytimeSeconds());
            };
            sender.sendMessage("§f" + (i + 1) + ". §b" + stats.name() + " §7- §f" + value);
        }
    }
}
