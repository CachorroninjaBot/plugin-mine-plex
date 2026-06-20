package com.haiz.servercore.activity;

import com.haiz.servercore.utils.NumberUtils;
import com.haiz.servercore.utils.TimeUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ActivityCommand implements CommandExecutor, TabCompleter {
    private static final String PREFIX = ChatColor.DARK_AQUA + "[Haiz Activity] " + ChatColor.RESET;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private final HaizActivityModule module;

    public ActivityCommand(HaizActivityModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("haiz.activity.use")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Voce nao tem permissao.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            status(sender);
            return true;
        }
        if (!module.isRunning()) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "O modulo esta desativado.");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "top" -> top(sender, args);
            case "player" -> player(sender, args);
            case "report" -> report(sender, args);
            case "reload" -> reload(sender);
            case "testdiscord" -> testDiscord(sender);
            default -> help(sender);
        }
        return true;
    }

    private void status(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "Haiz Activity");
        sender.sendMessage("Status: " + (module.isRunning() ? ChatColor.GREEN + "ativado" : ChatColor.RED + "desativado"));
        sender.sendMessage(ChatColor.RESET + "Storage: " + module.storageState());
        sender.sendMessage("Discord: " + (module.isRunning() && module.discord().isConfigured() ? "configurado" : "desativado/sem webhook"));
        sender.sendMessage("Sessoes abertas agora: " + module.onlineTracked());
        sender.sendMessage("Proximo relatorio diario: " + module.nextDailyTime());
        if (module.isRunning()) {
            module.queryAsync(() -> module.respond(sender,
                    List.of("Jogadores monitorados hoje: " + module.stats().today().uniquePlayers())));
        }
    }

    private void top(CommandSender sender, String[] args) {
        String period = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "today";
        String metric = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "playtime";
        if (!period.equals("today") && !period.equals("week")) {
            sender.sendMessage(PREFIX + "Use /haizactivity top <today|week> [playtime|commands|blocks|deaths|kills|mobs].");
            return;
        }
        String column = rankingColumn(metric);
        if (column == null) {
            sender.sendMessage(PREFIX + "Metrica invalida.");
            return;
        }
        sender.sendMessage(PREFIX + "Consultando ranking...");
        module.queryAsync(() -> {
            List<ActivityTopEntry> entries = period.equals("week")
                    ? module.stats().topWeek(column, module.config().weeklyRankingLimit())
                    : module.stats().topToday(column, module.config().dailyRankingLimit());
            List<String> lines = new ArrayList<>();
            lines.add(ChatColor.AQUA + "Top jogadores " + (period.equals("week") ? "da semana:" : "de hoje:"));
            if (entries.isEmpty()) {
                lines.add(ChatColor.GRAY + "Sem dados suficientes.");
            }
            for (int index = 0; index < entries.size(); index++) {
                ActivityTopEntry entry = entries.get(index);
                lines.add(ChatColor.YELLOW + String.valueOf(index + 1) + ". " + ChatColor.WHITE + entry.name()
                        + " - " + rankingValue(metric, entry.value()));
            }
            module.respond(sender, lines);
        });
    }

    private void player(CommandSender sender, String[] args) {
        if (!sender.hasPermission("haiz.activity.player")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Voce nao tem permissao.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "Use /haizactivity player <nick>.");
            return;
        }
        String name = args[1];
        sender.sendMessage(PREFIX + "Consultando " + name + "...");
        module.queryAsync(() -> {
            ActivityPlayerProfile profile = module.stats().player(name).orElse(null);
            if (profile == null) {
                module.respond(sender, List.of(PREFIX + ChatColor.RED + "Jogador nao encontrado."));
                return;
            }
            module.respond(sender, List.of(
                    ChatColor.AQUA + "Atividade de " + profile.name(),
                    "Primeira entrada: " + format(profile.firstJoin()),
                    "Ultima entrada: " + format(profile.lastJoin()),
                    "Ultima atividade: " + format(profile.lastSeen()),
                    "Tempo total online: " + TimeUtils.humanDuration(profile.totalPlaytimeSeconds()),
                    "Tempo hoje: " + TimeUtils.humanDuration(profile.todayPlaytimeSeconds()),
                    "Comandos hoje: " + NumberUtils.integer(profile.todayCommands()),
                    "Mortes hoje: " + NumberUtils.integer(profile.todayDeaths()),
                    "Blocos quebrados hoje: " + NumberUtils.integer(profile.todayBlocksBroken())
            ));
        });
    }

    private void report(CommandSender sender, String[] args) {
        if (!admin(sender) || args.length < 2) {
            if (args.length < 2) {
                sender.sendMessage(PREFIX + "Use /haizactivity report <daily|weekly>.");
            }
            return;
        }
        if (!module.discord().isConfigured()) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Webhook nao configurado.");
            return;
        }
        if (args[1].equalsIgnoreCase("daily")) {
            module.sendDailyReport();
        } else if (args[1].equalsIgnoreCase("weekly")) {
            module.sendWeeklyReport();
        } else {
            sender.sendMessage(PREFIX + "Use /haizactivity report <daily|weekly>.");
            return;
        }
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Relatorio agendado para envio.");
    }

    private void reload(CommandSender sender) {
        if (!admin(sender)) {
            return;
        }
        module.plugin().reloadConfig();
        module.reload();
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Configuracao recarregada.");
    }

    private void testDiscord(CommandSender sender) {
        if (!admin(sender)) {
            return;
        }
        if (!module.discord().isConfigured()) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Webhook nao configurado.");
            return;
        }
        module.sendTestDiscord();
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Embed de teste enviado.");
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("haiz.activity.admin")) {
            return true;
        }
        sender.sendMessage(PREFIX + ChatColor.RED + "Voce nao tem permissao administrativa.");
        return false;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(PREFIX + "/haizactivity status");
        sender.sendMessage("/haizactivity top <today|week> [metrica]");
        sender.sendMessage("/haizactivity player <nick>");
        sender.sendMessage("/haizactivity report <daily|weekly>");
        sender.sendMessage("/haizactivity reload | testdiscord");
    }

    private String format(long epochSeconds) {
        return epochSeconds <= 0 ? "sem registro" : DATE_TIME.format(Instant.ofEpochSecond(epochSeconds));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], "status", "top", "player", "report", "reload", "testdiscord");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return filter(args[1], "today", "week");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("top")) {
            return filter(args[2], "playtime", "commands", "blocks", "deaths", "kills", "mobs");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("report")) {
            return filter(args[1], "daily", "weekly");
        }
        return Collections.emptyList();
    }

    private List<String> filter(String input, String... values) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return Arrays.stream(values).filter(value -> value.startsWith(prefix)).toList();
    }

    private String rankingColumn(String metric) {
        return switch (metric) {
            case "playtime" -> "playtime_seconds";
            case "commands" -> "commands_used";
            case "blocks" -> "blocks_broken";
            case "deaths" -> "deaths";
            case "kills" -> "player_kills";
            case "mobs" -> "mob_kills";
            default -> null;
        };
    }

    private String rankingValue(String metric, long value) {
        return metric.equals("playtime") ? TimeUtils.humanDuration(value) : NumberUtils.integer(value);
    }
}
