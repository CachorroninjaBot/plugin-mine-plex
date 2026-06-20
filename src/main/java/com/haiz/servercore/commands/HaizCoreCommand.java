package com.haiz.servercore.commands;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.discord.DiscordEmbedFactory;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class HaizCoreCommand implements CommandExecutor, TabCompleter {
    private final HaizServerCore plugin;
    private final ReloadCommand reloadCommand;
    private final StatsCommand statsCommand;

    public HaizCoreCommand(HaizServerCore plugin) {
        this.plugin = plugin;
        this.reloadCommand = new ReloadCommand(plugin);
        this.statsCommand = new StatsCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (!has(sender, "haizcore.reload")) {
                    return true;
                }
                reloadCommand.execute(sender);
            }
            case "status" -> {
                if (!has(sender, "haizcore.admin")) {
                    return true;
                }
                status(sender);
            }
            case "testdiscord" -> {
                if (!has(sender, "haizcore.discord.test")) {
                    return true;
                }
                if (!plugin.discord().isOnline()) {
                    sender.sendMessage(plugin.messages().get("discord-disabled"));
                    return true;
                }
                plugin.discordLogs().testAllChannels();
                sender.sendMessage(plugin.messages().get("discord-test-sent"));
            }
            case "metrics" -> metrics(sender, args);
            case "report" -> report(sender, args);
            case "top" -> top(sender, args);
            default -> sender.sendMessage(plugin.messages().get("unknown-command"));
        }
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§bHaizServerCore §7- comandos");
        sender.sendMessage("§f/haizcore status §7- status dos modulos");
        sender.sendMessage("§f/haizcore reload §7- recarrega configs");
        sender.sendMessage("§f/haizcore testdiscord §7- envia embeds de teste");
        sender.sendMessage("§f/haizcore metrics §7- resumo atual");
        sender.sendMessage("§f/haizcore metrics player <nick> §7- metricas do jogador");
        sender.sendMessage("§f/haizcore report daily|weekly §7- gera relatorio");
        sender.sendMessage("§f/haizcore top playtime|commands|deaths|activity §7- rankings");
    }

    private void status(CommandSender sender) {
        sender.sendMessage(plugin.messages().raw("status-header"));
        sender.sendMessage("§7Discord: §f" + plugin.discord().getStateLabel());
        sender.sendMessage("§7Database: §f" + plugin.database().getStateLabel());
        sender.sendMessage("§7Metricas: §f" + (plugin.config().isMetricsEnabled() ? "on" : "off"));
        sender.sendMessage("§7Logs comandos: §f" + (plugin.config().isCommandLogEnabled() ? "on" : "off"));
        sender.sendMessage("§7Jogadores online: §f" + Bukkit.getOnlinePlayers().size());
    }

    private void metrics(CommandSender sender, String[] args) {
        if (!has(sender, "haizcore.metrics")) {
            return;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("player")) {
            statsCommand.player(sender, args[2]);
            return;
        }
        statsCommand.serverSummary(sender);
    }

    private void report(CommandSender sender, String[] args) {
        if (!has(sender, "haizcore.report")) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUse /haizcore report daily ou /haizcore report weekly.");
            return;
        }
        if (args[1].equalsIgnoreCase("weekly")) {
            plugin.metrics().sendWeeklyReport();
            sender.sendMessage("§aRelatorio semanal enviado para o Discord.");
        } else {
            plugin.metrics().sendDailyReport();
            sender.sendMessage("§aRelatorio diario enviado para o Discord.");
        }
    }

    private void top(CommandSender sender, String[] args) {
        if (!has(sender, "haizcore.metrics")) {
            return;
        }
        String type = args.length >= 2 ? args[1] : "playtime";
        statsCommand.top(sender, type);
    }

    private boolean has(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("haizcore.admin")) {
            return true;
        }
        sender.sendMessage(plugin.messages().get("no-permission"));
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("reload", "status", "testdiscord", "metrics", "report", "top"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("metrics")) {
            return filter(args[1], List.of("player"));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("metrics") && args[1].equalsIgnoreCase("player")) {
            return filter(args[2], Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).filter(java.util.Objects::nonNull).toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("report")) {
            return filter(args[1], List.of("daily", "weekly"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return filter(args[1], List.of("playtime", "commands", "deaths", "activity"));
        }
        return List.of();
    }

    private List<String> filter(String prefix, List<String> values) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }
}
