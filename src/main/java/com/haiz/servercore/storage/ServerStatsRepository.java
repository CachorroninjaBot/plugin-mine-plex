package com.haiz.servercore.storage;

import com.haiz.servercore.metrics.MetricsPeriodSummary;
import com.haiz.servercore.metrics.MetricsSnapshot;
import com.haiz.servercore.metrics.ServerPerformanceStats;
import com.haiz.servercore.utils.TimeUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public final class ServerStatsRepository {
    private final JavaPlugin plugin;
    private final SQLiteDatabase database;

    public ServerStatsRepository(JavaPlugin plugin, SQLiteDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    public synchronized void saveSnapshot(MetricsSnapshot snapshot) {
        try (PreparedStatement statement = database.connection().prepareStatement("""
                INSERT INTO server_snapshots(online_players, tps, mspt, unique_players_today,
                  loaded_chunks, entities, used_memory_mb, max_memory_mb, active_players, afk_players, avg_ping, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setInt(1, snapshot.onlinePlayers());
            statement.setDouble(2, snapshot.tps());
            statement.setDouble(3, snapshot.mspt());
            statement.setInt(4, snapshot.uniquePlayersToday());
            statement.setInt(5, snapshot.loadedChunks());
            statement.setInt(6, snapshot.entities());
            statement.setLong(7, snapshot.usedMemoryMb());
            statement.setLong(8, snapshot.maxMemoryMb());
            statement.setInt(9, snapshot.activePlayers());
            statement.setInt(10, snapshot.afkPlayers());
            statement.setDouble(11, snapshot.averagePing());
            statement.setLong(12, snapshot.createdAt());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Erro ao salvar snapshot: " + exception.getMessage());
        }
    }

    public synchronized void saveDailyStats(MetricsSnapshot snapshot) {
        ServerPerformanceStats performance = recentPerformance(TimeUtils.nowSeconds() - 86400L);
        try (PreparedStatement statement = database.connection().prepareStatement("""
                INSERT INTO daily_stats(day, unique_players, new_players, peak_online, total_playtime_seconds,
                  total_sessions, avg_session_seconds, commands_executed, chat_messages, deaths, kills,
                  blocks_broken, blocks_placed, distance_walked, avg_tps, min_tps, max_mspt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(day) DO UPDATE SET
                  unique_players=excluded.unique_players,
                  new_players=excluded.new_players,
                  peak_online=excluded.peak_online,
                  total_playtime_seconds=excluded.total_playtime_seconds,
                  total_sessions=excluded.total_sessions,
                  avg_session_seconds=excluded.avg_session_seconds,
                  commands_executed=excluded.commands_executed,
                  chat_messages=excluded.chat_messages,
                  deaths=excluded.deaths,
                  kills=excluded.kills,
                  blocks_broken=excluded.blocks_broken,
                  blocks_placed=excluded.blocks_placed,
                  distance_walked=excluded.distance_walked,
                  avg_tps=excluded.avg_tps,
                  min_tps=excluded.min_tps,
                  max_mspt=excluded.max_mspt
                """)) {
            statement.setString(1, snapshot.dayKey());
            statement.setInt(2, snapshot.uniquePlayersToday());
            statement.setInt(3, snapshot.newPlayersToday());
            statement.setInt(4, snapshot.peakOnlineToday());
            statement.setLong(5, snapshot.totalPlaytimeToday());
            statement.setLong(6, snapshot.totalSessionsToday());
            statement.setLong(7, snapshot.totalSessionsToday() == 0 ? 0 : snapshot.totalPlaytimeToday() / snapshot.totalSessionsToday());
            statement.setLong(8, snapshot.commandsToday());
            statement.setLong(9, snapshot.chatMessagesToday());
            statement.setLong(10, snapshot.deathsToday());
            statement.setLong(11, snapshot.killsToday());
            statement.setLong(12, snapshot.blocksBrokenToday());
            statement.setLong(13, snapshot.blocksPlacedToday());
            statement.setDouble(14, snapshot.distanceWalkedToday());
            statement.setDouble(15, performance.averageTps());
            statement.setDouble(16, performance.minimumTps());
            statement.setDouble(17, performance.maximumMspt());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Erro ao salvar daily_stats: " + exception.getMessage());
        }
    }

    public synchronized int allTimePeakOnline() {
        try (PreparedStatement statement = database.connection().prepareStatement("SELECT MAX(online_players) AS peak FROM server_snapshots");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt("peak") : 0;
        } catch (SQLException exception) {
            plugin.getLogger().warning("Erro ao consultar pico online: " + exception.getMessage());
            return 0;
        }
    }

    public synchronized ServerPerformanceStats recentPerformance(long sinceSeconds) {
        long until = TimeUtils.nowSeconds();
        try (PreparedStatement statement = database.connection().prepareStatement("""
                SELECT COUNT(*) AS samples,
                  AVG(CASE WHEN tps > 0 THEN tps END) AS avg_tps,
                  MIN(CASE WHEN tps > 0 THEN tps END) AS min_tps,
                  MAX(mspt) AS max_mspt,
                  AVG(online_players) AS avg_online,
                  MAX(online_players) AS peak_online
                FROM server_snapshots
                WHERE created_at >= ?
                """)) {
            statement.setLong(1, sinceSeconds);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int samples = resultSet.getInt("samples");
                    if (samples <= 0) {
                        return ServerPerformanceStats.empty(sinceSeconds, until);
                    }
                    return new ServerPerformanceStats(
                            samples,
                            resultSet.getDouble("avg_tps"),
                            resultSet.getDouble("min_tps"),
                            resultSet.getDouble("max_mspt"),
                            resultSet.getDouble("avg_online"),
                            resultSet.getInt("peak_online"),
                            sinceSeconds,
                            until
                    );
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Erro ao consultar performance recente: " + exception.getMessage());
        }
        return ServerPerformanceStats.empty(sinceSeconds, until);
    }

    public synchronized MetricsPeriodSummary periodSummary(int days) {
        String sinceDay = LocalDate.now().minusDays(Math.max(0, days - 1L)).toString();
        try (PreparedStatement statement = database.connection().prepareStatement("""
                SELECT COUNT(*) AS stored_days,
                  SUM(unique_players) AS unique_players,
                  SUM(new_players) AS new_players,
                  MAX(peak_online) AS peak_online,
                  SUM(total_playtime_seconds) AS total_playtime_seconds,
                  SUM(total_sessions) AS total_sessions,
                  SUM(commands_executed) AS commands_executed,
                  SUM(chat_messages) AS chat_messages,
                  SUM(deaths) AS deaths,
                  SUM(kills) AS kills,
                  SUM(blocks_broken) AS blocks_broken,
                  SUM(blocks_placed) AS blocks_placed,
                  SUM(distance_walked) AS distance_walked,
                  AVG(CASE WHEN avg_tps > 0 THEN avg_tps END) AS avg_tps,
                  MIN(CASE WHEN min_tps > 0 THEN min_tps END) AS min_tps,
                  MAX(max_mspt) AS max_mspt
                FROM daily_stats
                WHERE day >= ?
                """)) {
            statement.setString(1, sinceDay);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getInt("stored_days") > 0) {
                    long sessions = resultSet.getLong("total_sessions");
                    long playtime = resultSet.getLong("total_playtime_seconds");
                    return new MetricsPeriodSummary(
                            days,
                            resultSet.getInt("stored_days"),
                            resultSet.getLong("unique_players"),
                            resultSet.getLong("new_players"),
                            resultSet.getInt("peak_online"),
                            playtime,
                            sessions,
                            sessions <= 0 ? 0 : playtime / sessions,
                            resultSet.getLong("commands_executed"),
                            resultSet.getLong("chat_messages"),
                            resultSet.getLong("deaths"),
                            resultSet.getLong("kills"),
                            resultSet.getLong("blocks_broken"),
                            resultSet.getLong("blocks_placed"),
                            resultSet.getDouble("distance_walked"),
                            resultSet.getDouble("avg_tps"),
                            resultSet.getDouble("min_tps"),
                            resultSet.getDouble("max_mspt")
                    );
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Erro ao consultar resumo de periodo: " + exception.getMessage());
        }
        return MetricsPeriodSummary.empty(days);
    }
}
