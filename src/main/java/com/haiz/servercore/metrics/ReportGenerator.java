package com.haiz.servercore.metrics;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.discord.DiscordEmbedFactory;
import com.haiz.servercore.storage.DatabaseManager;
import com.haiz.servercore.utils.TimeUtils;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

public final class ReportGenerator {
    private final HaizServerCore plugin;
    private final DatabaseManager database;

    public ReportGenerator(HaizServerCore plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    public MessageEmbed dailyReport(MetricsSnapshot snapshot) {
        List<PlayerStats> topPlaytime = database.players().top("total_playtime_seconds", 5);
        List<PlayerStats> topCommands = database.players().top("total_commands", 5);
        List<PlayerStats> topChat = database.players().top("total_chat_messages", 5);
        ServerPerformanceStats performance = database.serverStats().recentPerformance(TimeUtils.nowSeconds() - 86400L);
        MetricsPeriodSummary yesterday = database.serverStats().periodSummary(2);
        return DiscordEmbedFactory.dailyReport(snapshot, topPlaytime, topCommands, topChat, performance, yesterday);
    }

    public MessageEmbed weeklyReport(MetricsSnapshot snapshot) {
        List<PlayerStats> topPlaytime = database.players().top("total_playtime_seconds", 5);
        List<PlayerStats> topCommands = database.players().top("total_commands", 5);
        List<PlayerStats> topChat = database.players().top("total_chat_messages", 5);
        MetricsPeriodSummary summary = database.serverStats().periodSummary(7);
        String suggestion = automaticSuggestion(snapshot, summary);
        return DiscordEmbedFactory.weeklyReport(snapshot, topPlaytime, topCommands, topChat, summary, suggestion);
    }

    public String textSummary(MetricsSnapshot snapshot) {
        return "Online: " + snapshot.onlinePlayers()
                + "/" + snapshot.maxPlayers()
                + "\nUnicos hoje: " + snapshot.uniquePlayersToday()
                + "\nNovos hoje: " + snapshot.newPlayersToday()
                + "\nPico hoje: " + snapshot.peakOnlineToday()
                + "\nTPS: " + (snapshot.tps() < 0 ? "indisponivel" : String.format(java.util.Locale.US, "%.2f", snapshot.tps()))
                + "\nMSPT: " + String.format(java.util.Locale.US, "%.1f", snapshot.mspt())
                + "\nAtivos/AFK: " + snapshot.activePlayers() + "/" + snapshot.afkPlayers()
                + "\nSessoes hoje: " + snapshot.totalSessionsToday()
                + "\nTempo jogado hoje: " + TimeUtils.humanDuration(snapshot.totalPlaytimeToday())
                + "\nComandos hoje: " + snapshot.commandsToday()
                + "\nChat hoje: " + snapshot.chatMessagesToday()
                + "\nBlocos hoje: " + (snapshot.blocksBrokenToday() + snapshot.blocksPlacedToday());
    }

    private String automaticSuggestion(MetricsSnapshot snapshot, MetricsPeriodSummary summary) {
        if (snapshot.tps() > 0 && snapshot.tps() < 18.5) {
            return "TPS abaixo do ideal; revise entidades, farms e plugins mais pesados antes do proximo pico.";
        }
        if (snapshot.maxMemoryMb() > 0 && snapshot.usedMemoryMb() * 100.0 / snapshot.maxMemoryMb() > 85) {
            return "Memoria alta; considere ajustar heap, reduzir chunks carregados ou revisar vazamentos de plugins.";
        }
        if (summary.storedDays() >= 3 && summary.totalSessions() > 0 && summary.averageSessionSeconds() < 900) {
            return "A media semanal de sessao esta curta; eventos diarios e recompensas de permanencia podem melhorar retencao.";
        }
        if (snapshot.newPlayersToday() > 0 && snapshot.totalPlaytimeToday() / Math.max(1, snapshot.totalSessionsToday()) < 600) {
            return "Jogadores novos estao saindo rapido; considere criar recompensas iniciais.";
        }
        if (snapshot.totalSessionsToday() > 0 && snapshot.totalPlaytimeToday() / snapshot.totalSessionsToday() < 1200) {
            return "A media de sessao caiu; talvez falte evento ou objetivo.";
        }
        return "Seu servidor teve bom movimento; observe os horarios de pico para agendar eventos.";
    }
}
