package com.haiz.servercore.activity;

import com.haiz.servercore.utils.NumberUtils;
import com.haiz.servercore.utils.TimeUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ActivityReportService {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaPlugin plugin;
    private final ActivityStatsService stats;
    private final ActivityInsightService insights;
    private final ActivityDiscordService discord;
    private volatile ActivityConfig config;

    public ActivityReportService(JavaPlugin plugin, ActivityStatsService stats, ActivityInsightService insights,
                                 ActivityDiscordService discord, ActivityConfig config) {
        this.plugin = plugin;
        this.stats = stats;
        this.insights = insights;
        this.discord = discord;
        this.config = config;
    }

    public void reload(ActivityConfig config) {
        this.config = config;
    }

    public CompletableFuture<Boolean> sendDaily() {
        ActivityPeriodSummary today = stats.today();
        ActivityPeriodSummary yesterday = stats.yesterday();
        List<ActivityTopEntry> top = stats.topToday("playtime_seconds", config.dailyRankingLimit());
        double retention = stats.retention(today.fromDate(), today.toDate());
        List<String> generatedInsights = insights.generate(today, yesterday, top, retention);

        ActivityEmbed embed = new ActivityEmbed(
                "Relatorio Diario - Haiz Activity",
                "Atividade de " + LocalDate.now().format(DATE),
                0x5865F2
        ).addField("Jogadores", "**Unicos:** " + today.uniquePlayers()
                        + "\n**Novos:** " + today.newPlayers()
                        + "\n**Media online:** " + decimal(today.averageOnline()), true)
                .addField("Tempo", "**Total:** " + TimeUtils.humanDuration(today.totalPlaytimeSeconds())
                        + "\n**Media/jogador:** " + TimeUtils.humanDuration(today.averagePlaytimeSeconds())
                        + "\n**Pico:** " + today.peakOnline(), true)
                .addField("Horario mais ativo", busiestHour(today.busiestHour()), true)
                .addField("Top jogadores", formatTop(top), false)
                .addField("Combate", "**Mortes:** " + NumberUtils.integer(today.deaths())
                        + "\n**Kills PvP:** " + NumberUtils.integer(today.playerKills())
                        + "\n**Mobs mortos:** " + NumberUtils.integer(today.mobKills()), true)
                .addField("Mundo", "**Quebrados:** " + NumberUtils.integer(today.blocksBroken())
                        + "\n**Colocados:** " + NumberUtils.integer(today.blocksPlaced())
                        + "\n**Distancia:** " + distance(today.distanceWalked()), true)
                .addField("Comparacao com ontem", comparison(today, yesterday), true)
                .addField("Haiz Insight", formatInsights(generatedInsights), false);
        return send(embed, "diario");
    }

    public CompletableFuture<Boolean> sendWeekly() {
        ActivityPeriodSummary week = stats.week();
        ActivityPeriodSummary previous = stats.previousWeek();
        List<ActivityTopEntry> top = stats.topWeek("playtime_seconds", config.weeklyRankingLimit());
        double retention = stats.retention(week.fromDate(), week.toDate());
        String busiestDay = stats.busiestDay(week.fromDate(), week.toDate());
        List<String> generatedInsights = insights.generate(week, previous, top, retention);

        ActivityEmbed embed = new ActivityEmbed(
                "Relatorio Semanal - Haiz Activity",
                "Periodo: " + formatDate(week.fromDate()) + " a " + formatDate(week.toDate()),
                0x9B59B6
        ).addField("Resumo", "**Jogadores unicos:** " + week.uniquePlayers()
                        + "\n**Novos:** " + week.newPlayers()
                        + "\n**Tempo total:** " + TimeUtils.humanDuration(week.totalPlaytimeSeconds())
                        + "\n**Pico online:** " + week.peakOnline(), false)
                .addField("Maior movimento", "**Dia:** " + dayName(busiestDay)
                        + "\n**Horario:** " + busiestHour(week.busiestHour()), true)
                .addField("Retencao de novos", decimal(retention) + "% ficaram 10+ minutos", true)
                .addField("Comparacao semanal", comparison(week, previous), true)
                .addField("Top da semana", formatTop(top), false)
                .addField("Haiz Insight", formatInsights(generatedInsights), false);
        return send(embed, "semanal");
    }

    private CompletableFuture<Boolean> send(ActivityEmbed embed, String type) {
        return discord.send(embed).thenApply(success -> {
            if (success) {
                plugin.getLogger().info("[HaizActivity] Relatorio " + type + " enviado ao Discord.");
            }
            return success;
        });
    }

    private String formatTop(List<ActivityTopEntry> top) {
        if (top.isEmpty()) {
            return "Sem dados suficientes.";
        }
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < top.size(); index++) {
            ActivityTopEntry entry = top.get(index);
            result.append(index + 1).append(". **").append(entry.name()).append("** - ")
                    .append(TimeUtils.humanDuration(entry.value())).append('\n');
        }
        return result.toString();
    }

    private String comparison(ActivityPeriodSummary current, ActivityPeriodSummary previous) {
        double activity = stats.percentChange(current.totalPlaytimeSeconds(), previous.totalPlaytimeSeconds());
        long uniqueDifference = current.uniquePlayers() - previous.uniquePlayers();
        long timeDifference = current.totalPlaytimeSeconds() - previous.totalPlaytimeSeconds();
        return "**Atividade:** " + signedPercent(activity)
                + "\n**Jogadores:** " + signed(uniqueDifference)
                + "\n**Tempo:** " + signedDuration(timeDifference);
    }

    private String formatInsights(List<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() + value.length() > 950) {
                break;
            }
            result.append("- ").append(value).append('\n');
        }
        return result.toString();
    }

    private String busiestHour(int hour) {
        if (hour < 0) {
            return "Sem dados";
        }
        return String.format("%02dh-%02dh", hour, (hour + 1) % 24);
    }

    private String formatDate(String value) {
        return LocalDate.parse(value).format(DATE);
    }

    private String dayName(String value) {
        if (value == null || value.isBlank()) {
            return "Sem dados";
        }
        return LocalDate.parse(value).getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);
    }

    private String signedPercent(double value) {
        return (value >= 0 ? "+" : "") + Math.round(value) + "%";
    }

    private String signed(long value) {
        return (value >= 0 ? "+" : "") + value;
    }

    private String signedDuration(long value) {
        return (value >= 0 ? "+" : "-") + TimeUtils.humanDuration(Math.abs(value));
    }

    private String decimal(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private String distance(double blocks) {
        return blocks >= 1000
                ? String.format(Locale.US, "%.2f km", blocks / 1000.0)
                : String.format(Locale.US, "%.0f blocos", blocks);
    }
}
