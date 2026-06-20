package com.haiz.servercore.discord;

import com.haiz.servercore.metrics.MetricsSnapshot;
import com.haiz.servercore.metrics.MetricsPeriodSummary;
import com.haiz.servercore.metrics.PlayerStats;
import com.haiz.servercore.metrics.ServerPerformanceStats;
import com.haiz.servercore.utils.NumberUtils;
import com.haiz.servercore.utils.TimeUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DiscordEmbedFactory {
    private static final Color BLUE = new Color(0x3498DB);
    private static final Color GREEN = new Color(0x00AA00);
    private static final Color RED = new Color(0xFF0000);
    private static final Color ORANGE = new Color(0xE67E22);
    private static final Color PURPLE = new Color(0x9B59B6);
    private static final Color CHAT = new Color(0x5865F2);
    private static final String FOOTER = "HaizServerCore | Minecraft Analytics";

    private DiscordEmbedFactory() {
    }

    public static MessageEmbed info(String title, String description) {
        return base(BLUE).setTitle(title).setDescription(description).build();
    }

    public static DiscordLogMessage join(String player, UUID uuid, boolean firstJoin, int online) {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(GREEN)
                .setAuthor(player + " Entrou no servidor", null, headUrl(uuid))
                .setDescription("**\uD83D\uDC65 Online:** " + online + "\n"
                        + "**\u2728 Status:** " + (firstJoin ? "Primeiro login" : "Retorno"))
                .build();
        return withComponents(embed, ActionRow.of(
                Button.success("haiz:join:status", "Entrada").asDisabled(),
                Button.secondary("haiz:join:online", "Online: " + online).asDisabled(),
                Button.secondary("haiz:join:type", firstJoin ? "Novo jogador" : "Jogador retornando").asDisabled()
        ));
    }

    public static DiscordLogMessage leave(String player, UUID uuid, int online) {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(RED)
                .setAuthor(player + " Saiu do servidor", null, headUrl(uuid))
                .setDescription("**\uD83D\uDC65 Online:** " + online)
                .build();
        return withComponents(embed, ActionRow.of(
                Button.danger("haiz:leave:status", "Saida").asDisabled(),
                Button.secondary("haiz:leave:online", "Online: " + online).asDisabled()
        ));
    }

    public static MessageEmbed command(String title, String details) {
        return base(ORANGE).setTitle(title).setDescription(details).build();
    }

    public static DiscordLogMessage playerCommand(String player, String group, String command) {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(GREEN)
                .setTitle("\u2705 Comando Executado")
                .setDescription("**\uD83D\uDC64 Player:** " + player + "\n"
                        + "**\uD83D\uDC51 Grupo:** " + group + "\n"
                        + "**\uD83D\uDEE0\uFE0F Comando:** `" + command + "`")
                .setFooter("Logs de Comandos")
                .setTimestamp(Instant.now())
                .build();
        return withComponents(embed, ActionRow.of(
                Button.success("haiz:command:status", "Executado").asDisabled(),
                Button.secondary("haiz:command:player", buttonLabel("Player: " + player)).asDisabled(),
                Button.secondary("haiz:command:group", buttonLabel("Grupo: " + group)).asDisabled()
        ));
    }

    public static DiscordLogMessage consoleCommand(String executor, String command) {
        MessageEmbed embed = base(ORANGE)
                .setTitle("\uD83D\uDDA5\uFE0F Comando do Console")
                .setDescription("**Executor:** " + executor + "\n"
                        + "**Comando:** `" + command + "`")
                .setFooter("Logs de Comandos")
                .build();
        return withComponents(embed, ActionRow.of(
                Button.primary("haiz:console:source", "Console").asDisabled(),
                Button.secondary("haiz:console:executor", buttonLabel("Executor: " + executor)).asDisabled()
        ));
    }

    public static DiscordLogMessage chat(String player, String message) {
        MessageEmbed embed = base(CHAT)
                .setTitle("\uD83D\uDCAC Mensagem no Chat")
                .setDescription("**\uD83D\uDC64 Player:** " + player + "\n"
                        + "**\uD83D\uDCAC Mensagem:** " + message)
                .setFooter("Logs do Chat")
                .build();
        return withComponents(embed, ActionRow.of(
                Button.primary("haiz:chat:type", "Chat").asDisabled(),
                Button.secondary("haiz:chat:player", buttonLabel("Player: " + player)).asDisabled()
        ));
    }

    public static DiscordLogMessage serverStarted(int online) {
        MessageEmbed embed = base(GREEN)
                .setTitle("\uD83D\uDFE2 Servidor Ligado")
                .setDescription("**Status:** Online\n**\uD83D\uDC65 Jogadores online:** " + online)
                .setFooter("Logs de Status")
                .build();
        return withComponents(embed, ActionRow.of(
                Button.success("haiz:server:start", "Ligado").asDisabled(),
                Button.secondary("haiz:server:start-online", "Online: " + online).asDisabled()
        ));
    }

    public static DiscordLogMessage serverStopped(int online) {
        MessageEmbed embed = base(RED)
                .setTitle("\uD83D\uDD34 Servidor Desligado")
                .setDescription("**Status:** Offline\n**\uD83D\uDC65 Jogadores online no desligamento:** " + online)
                .setFooter("Logs de Status")
                .build();
        return withComponents(embed, ActionRow.of(
                Button.danger("haiz:server:stop", "Desligado").asDisabled(),
                Button.secondary("haiz:server:stop-online", "Online: " + online).asDisabled()
        ));
    }

    public static MessageEmbed alert(String title, String details) {
        return base(RED).setTitle("Alerta: " + title).setDescription(details).build();
    }

    public static MessageEmbed metrics(String title, String details) {
        return base(PURPLE).setTitle(title).setDescription(details).build();
    }

    public static MessageEmbed playerStats(PlayerStats stats) {
        return base(PURPLE)
                .setTitle("Metricas de " + stats.name())
                .addField("Status", stats.status(TimeUtils.nowSeconds()), true)
                .addField("Tempo total", TimeUtils.humanDuration(stats.totalPlaytimeSeconds()), true)
                .addField("Sessoes", NumberUtils.integer(stats.totalSessions()), true)
                .addField("Media por sessao", TimeUtils.humanDuration(stats.averageSessionSeconds()), true)
                .addField("Comandos", NumberUtils.integer(stats.totalCommands()), true)
                .addField("Chat", NumberUtils.integer(stats.totalChatMessages()), true)
                .addField("Mortes", NumberUtils.integer(stats.totalDeaths()), true)
                .addField("Kills", NumberUtils.integer(stats.totalKills()), true)
                .addField("Blocos", "Quebrados: " + stats.blocksBroken() + "\nColocados: " + stats.blocksPlaced(), true)
                .addField("Primeiro login", TimeUtils.formatDateTime(stats.firstJoin()), true)
                .addField("Ultima entrada", TimeUtils.formatDateTime(stats.lastJoin()), true)
                .build();
    }

    public static MessageEmbed dailyReport(MetricsSnapshot snapshot, List<PlayerStats> topPlaytime, List<PlayerStats> topCommands,
                                           List<PlayerStats> topChat, ServerPerformanceStats performance, MetricsPeriodSummary recentDays) {
        return base(PURPLE)
                .setTitle("\uD83D\uDCCA Relatorio Diario do Servidor")
                .setDescription("Resumo operacional com saude, atividade e consumo do servidor.")
                .addField("Saude agora", healthBlock(snapshot), false)
                .addField("Jogadores", "**Online:** " + snapshot.onlinePlayers() + "/" + snapshot.maxPlayers()
                        + "\n**Ativos/AFK:** " + snapshot.activePlayers() + "/" + snapshot.afkPlayers()
                        + "\n**Unicos hoje:** " + snapshot.uniquePlayersToday()
                        + "\n**Novos:** " + snapshot.newPlayersToday()
                        + "\n**Pico:** " + snapshot.peakOnlineToday(), true)
                .addField("Engajamento", "**Tempo jogado:** " + TimeUtils.humanDuration(snapshot.totalPlaytimeToday())
                        + "\n**Sessoes:** " + snapshot.totalSessionsToday()
                        + "\n**Media/sessao:** " + TimeUtils.humanDuration(avg(snapshot.totalPlaytimeToday(), snapshot.totalSessionsToday()))
                        + "\n**Comandos:** " + NumberUtils.integer(snapshot.commandsToday())
                        + "\n**Chat:** " + NumberUtils.integer(snapshot.chatMessagesToday()), true)
                .addField("Gameplay", "**Mortes/Kills:** " + snapshot.deathsToday() + "/" + snapshot.killsToday()
                        + "\n**Blocos:** " + NumberUtils.integer(snapshot.blocksBrokenToday() + snapshot.blocksPlacedToday())
                        + "\n**Distancia:** " + distance(snapshot.distanceWalkedToday())
                        + "\n**Ping medio:** " + String.format(java.util.Locale.US, "%.0f ms", snapshot.averagePing()), true)
                .addField("Recursos", "**Memoria:** " + snapshot.usedMemoryMb() + "/" + snapshot.maxMemoryMb() + " MB " + memoryBar(snapshot)
                        + "\n**Mundos:** " + snapshot.loadedWorlds()
                        + "\n**Chunks:** " + NumberUtils.integer(snapshot.loadedChunks())
                        + "\n**Entidades:** " + NumberUtils.integer(snapshot.entities())
                        + "\n**Uptime:** " + TimeUtils.humanDuration(snapshot.uptimeSeconds()), false)
                .addField("Performance 24h", performanceBlock(performance), false)
                .addField("Top tempo", formatTop(topPlaytime, "playtime"), true)
                .addField("Top comandos", formatTop(topCommands, "commands"), true)
                .addField("Top chat", formatTop(topChat, "chat"), true)
                .addField("Tendencia recente", trendBlock(recentDays), false)
                .build();
    }

    public static MessageEmbed weeklyReport(MetricsSnapshot snapshot, List<PlayerStats> topPlaytime, List<PlayerStats> topCommands,
                                            List<PlayerStats> topChat, MetricsPeriodSummary summary, String suggestion) {
        return base(PURPLE)
                .setTitle("\uD83D\uDCC8 Relatorio Semanal do Servidor")
                .setDescription("Periodo analisado: " + summary.storedDays() + "/" + summary.requestedDays() + " dias com dados salvos.")
                .addField("Resumo semanal", "**Unicos somados:** " + NumberUtils.integer(summary.uniquePlayers())
                        + "\n**Novos:** " + NumberUtils.integer(summary.newPlayers())
                        + "\n**Pico online:** " + summary.peakOnline()
                        + "\n**Tempo jogado:** " + TimeUtils.humanDuration(summary.totalPlaytimeSeconds())
                        + "\n**Sessoes:** " + NumberUtils.integer(summary.totalSessions())
                        + "\n**Media/sessao:** " + TimeUtils.humanDuration(summary.averageSessionSeconds()), false)
                .addField("Atividade", "**Comandos:** " + NumberUtils.integer(summary.commandsExecuted())
                        + "\n**Mensagens:** " + NumberUtils.integer(summary.chatMessages())
                        + "\n**Mortes/Kills:** " + summary.deaths() + "/" + summary.kills()
                        + "\n**Blocos:** " + NumberUtils.integer(summary.blocksBroken() + summary.blocksPlaced())
                        + "\n**Distancia:** " + distance(summary.distanceWalked()), true)
                .addField("Performance", "**TPS medio:** " + tps(summary.averageTps())
                        + "\n**Pior TPS:** " + tps(summary.minimumTps())
                        + "\n**Maior MSPT:** " + String.format(java.util.Locale.US, "%.1f", summary.maximumMspt())
                        + "\n**Saude atual:** " + healthLabel(snapshot), true)
                .addField("Top tempo", formatTop(topPlaytime, "playtime"), true)
                .addField("Top comandos", formatTop(topCommands, "commands"), true)
                .addField("Top chat", formatTop(topChat, "chat"), true)
                .addField("Sugestao automatica", suggestion, false)
                .build();
    }

    private static EmbedBuilder base(Color color) {
        return new EmbedBuilder()
                .setColor(color)
                .setFooter(FOOTER)
                .setTimestamp(Instant.now());
    }

    private static long avg(long total, long count) {
        return count <= 0 ? 0 : total / count;
    }

    private static String formatTop(List<PlayerStats> players, String type) {
        if (players.isEmpty()) {
            return "Sem dados ainda.";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < players.size(); index++) {
            PlayerStats stats = players.get(index);
            String value = switch (type) {
                case "commands" -> NumberUtils.integer(stats.totalCommands()) + " comandos";
                case "chat" -> NumberUtils.integer(stats.totalChatMessages()) + " mensagens";
                default -> TimeUtils.humanDuration(stats.totalPlaytimeSeconds());
            };
            builder.append(index + 1).append(". ").append(stats.name()).append(" - ").append(value).append('\n');
        }
        return builder.toString();
    }

    private static DiscordLogMessage withComponents(MessageEmbed embed, ActionRow row) {
        return new DiscordLogMessage(embed, List.of(row));
    }

    private static String headUrl(UUID uuid) {
        String id = uuid == null ? "0000000000000000000901f953f4f24d" : uuid.toString().replace("-", "");
        return "https://crafthead.net/helm/" + id + "/128";
    }

    private static String buttonLabel(String value) {
        if (value == null || value.isBlank()) {
            return "Info";
        }
        return value.length() <= 80 ? value : value.substring(0, 77) + "...";
    }

    private static String healthBlock(MetricsSnapshot snapshot) {
        return "**Status:** " + healthLabel(snapshot)
                + "\n**TPS:** " + tps(snapshot.tps()) + " " + tpsBar(snapshot.tps())
                + "\n**MSPT:** " + String.format(java.util.Locale.US, "%.1f", snapshot.mspt())
                + "\n**Memoria:** " + snapshot.usedMemoryMb() + "/" + snapshot.maxMemoryMb() + " MB"
                + "\n**Ping medio:** " + String.format(java.util.Locale.US, "%.0f ms", snapshot.averagePing());
    }

    private static String healthLabel(MetricsSnapshot snapshot) {
        if (snapshot.tps() > 0 && snapshot.tps() < 16.0) {
            return "Critico";
        }
        if (snapshot.tps() > 0 && snapshot.tps() < 18.5) {
            return "Atencao";
        }
        if (snapshot.maxMemoryMb() > 0 && snapshot.usedMemoryMb() * 100.0 / snapshot.maxMemoryMb() > 90) {
            return "Memoria alta";
        }
        return "Estavel";
    }

    private static String performanceBlock(ServerPerformanceStats performance) {
        if (performance.samples() <= 0) {
            return "Ainda sem snapshots suficientes para tendencia.";
        }
        return "**Amostras:** " + performance.samples()
                + "\n**TPS medio:** " + tps(performance.averageTps())
                + "\n**Pior TPS:** " + tps(performance.minimumTps())
                + "\n**Maior MSPT:** " + String.format(java.util.Locale.US, "%.1f", performance.maximumMspt())
                + "\n**Online medio/pico:** " + String.format(java.util.Locale.US, "%.1f", performance.averageOnline()) + "/" + performance.peakOnline();
    }

    private static String trendBlock(MetricsPeriodSummary summary) {
        if (summary.storedDays() <= 0) {
            return "Sem historico salvo suficiente ainda.";
        }
        return "**Dias salvos:** " + summary.storedDays()
                + "\n**Tempo acumulado:** " + TimeUtils.humanDuration(summary.totalPlaytimeSeconds())
                + "\n**Comandos + chat:** " + NumberUtils.integer(summary.commandsExecuted() + summary.chatMessages())
                + "\n**TPS medio:** " + tps(summary.averageTps());
    }

    private static String tps(double value) {
        return value <= 0 ? "indisponivel" : String.format(java.util.Locale.US, "%.2f", value);
    }

    private static String distance(double blocks) {
        if (blocks >= 1000) {
            return String.format(java.util.Locale.US, "%.2f km", blocks / 1000.0);
        }
        return String.format(java.util.Locale.US, "%.0f blocos", blocks);
    }

    private static String memoryBar(MetricsSnapshot snapshot) {
        if (snapshot.maxMemoryMb() <= 0) {
            return "";
        }
        return bar(snapshot.usedMemoryMb() / (double) snapshot.maxMemoryMb());
    }

    private static String tpsBar(double tps) {
        if (tps <= 0) {
            return "";
        }
        return bar(Math.min(1.0, tps / 20.0));
    }

    private static String bar(double ratio) {
        int filled = (int) Math.round(Math.max(0, Math.min(1, ratio)) * 10);
        return "[" + "#".repeat(filled) + "-".repeat(10 - filled) + "]";
    }
}
