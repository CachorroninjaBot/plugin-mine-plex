package com.haiz.servercore.activity;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ActivityStorage {
    private final JavaPlugin plugin;
    private ActivityConfig config;
    private Connection connection;
    private volatile boolean available;

    public ActivityStorage(JavaPlugin plugin, ActivityConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public synchronized void reload(ActivityConfig config) {
        this.config = config;
    }

    public synchronized void open() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("[HaizActivity] Nao foi possivel criar a pasta de dados.");
            }
            File file = new File(plugin.getDataFolder(), config.storageFile());
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA foreign_keys=ON");
            }
            createTables();
            available = true;
            plugin.getLogger().info("[HaizActivity] SQLite conectado.");
        } catch (SQLException exception) {
            available = false;
            plugin.getLogger().warning("[HaizActivity] SQLite falhou: " + exception.getMessage());
        }
    }

    public synchronized void close() {
        available = false;
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao fechar SQLite: " + exception.getMessage());
        }
        connection = null;
    }

    public boolean isAvailable() {
        return available;
    }

    private synchronized Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            open();
        }
        if (connection == null) {
            throw new SQLException("SQLite indisponivel");
        }
        return connection;
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS players (
                      uuid TEXT PRIMARY KEY,
                      name TEXT,
                      first_join INTEGER,
                      last_join INTEGER,
                      last_seen INTEGER,
                      total_playtime_seconds INTEGER DEFAULT 0,
                      sessions INTEGER DEFAULT 0,
                      is_new_player INTEGER DEFAULT 0
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS activity_daily (
                      date TEXT,
                      uuid TEXT,
                      name TEXT,
                      playtime_seconds INTEGER DEFAULT 0,
                      joins INTEGER DEFAULT 0,
                      commands_used INTEGER DEFAULT 0,
                      deaths INTEGER DEFAULT 0,
                      player_kills INTEGER DEFAULT 0,
                      mob_kills INTEGER DEFAULT 0,
                      blocks_broken INTEGER DEFAULT 0,
                      blocks_placed INTEGER DEFAULT 0,
                      advancements INTEGER DEFAULT 0,
                      distance_walked DOUBLE DEFAULT 0,
                      PRIMARY KEY(date, uuid)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS activity_sessions (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      uuid TEXT,
                      name TEXT,
                      join_time INTEGER,
                      quit_time INTEGER,
                      duration_seconds INTEGER
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS activity_commands (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      uuid TEXT,
                      name TEXT,
                      command TEXT,
                      timestamp INTEGER
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS activity_snapshots (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      timestamp INTEGER,
                      online_players INTEGER,
                      tps DOUBLE,
                      memory_used INTEGER
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_activity_daily_date ON activity_daily(date)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_activity_sessions_uuid ON activity_sessions(uuid)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_activity_commands_timestamp ON activity_commands(timestamp)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_activity_snapshots_timestamp ON activity_snapshots(timestamp)");
        }
    }

    public synchronized void upsertJoin(UUID uuid, String name, long timestamp, boolean newPlayer) {
        if (!available) {
            return;
        }
        execute("""
                INSERT INTO players(uuid, name, first_join, last_join, last_seen, sessions, is_new_player)
                VALUES (?, ?, ?, ?, ?, 0, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                  name=excluded.name,
                  last_join=excluded.last_join,
                  last_seen=excluded.last_seen
                """, statement -> {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setLong(3, timestamp);
            statement.setLong(4, timestamp);
            statement.setLong(5, timestamp);
            statement.setInt(6, newPlayer ? 1 : 0);
        });
    }

    public synchronized boolean playerExists(UUID uuid) {
        if (!available) {
            return false;
        }
        try (PreparedStatement statement = connection().prepareStatement("SELECT 1 FROM players WHERE uuid=? LIMIT 1")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao consultar jogador: " + exception.getMessage());
            return false;
        }
    }

    public synchronized boolean saveBatch(List<ActivityDailyRecord> dailyRecords,
                                          List<ActivityClosedSession> sessions,
                                          List<ActivityCommandLog> commands) {
        if (!available || (dailyRecords.isEmpty() && sessions.isEmpty() && commands.isEmpty())) {
            return available;
        }
        try {
            Connection current = connection();
            boolean previousAutoCommit = current.getAutoCommit();
            current.setAutoCommit(false);
            try {
                saveDailyRecords(current, dailyRecords);
                saveSessions(current, sessions);
                saveCommands(current, commands);
                current.commit();
            } catch (SQLException exception) {
                current.rollback();
                throw exception;
            } finally {
                current.setAutoCommit(previousAutoCommit);
            }
            return true;
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao salvar lote: " + exception.getMessage());
            return false;
        }
    }

    private void saveDailyRecords(Connection current, List<ActivityDailyRecord> records) throws SQLException {
        if (records.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = current.prepareStatement("""
                INSERT INTO activity_daily(date, uuid, name, playtime_seconds, joins, commands_used, deaths,
                  player_kills, mob_kills, blocks_broken, blocks_placed, advancements, distance_walked)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(date, uuid) DO UPDATE SET
                  name=excluded.name,
                  playtime_seconds=activity_daily.playtime_seconds + excluded.playtime_seconds,
                  joins=activity_daily.joins + excluded.joins,
                  commands_used=activity_daily.commands_used + excluded.commands_used,
                  deaths=activity_daily.deaths + excluded.deaths,
                  player_kills=activity_daily.player_kills + excluded.player_kills,
                  mob_kills=activity_daily.mob_kills + excluded.mob_kills,
                  blocks_broken=activity_daily.blocks_broken + excluded.blocks_broken,
                  blocks_placed=activity_daily.blocks_placed + excluded.blocks_placed,
                  advancements=activity_daily.advancements + excluded.advancements,
                  distance_walked=activity_daily.distance_walked + excluded.distance_walked
                """)) {
            for (ActivityDailyRecord record : records) {
                statement.setString(1, record.date());
                statement.setString(2, record.uuid().toString());
                statement.setString(3, record.name());
                statement.setLong(4, record.playtimeSeconds());
                statement.setLong(5, record.joins());
                statement.setLong(6, record.commandsUsed());
                statement.setLong(7, record.deaths());
                statement.setLong(8, record.playerKills());
                statement.setLong(9, record.mobKills());
                statement.setLong(10, record.blocksBroken());
                statement.setLong(11, record.blocksPlaced());
                statement.setLong(12, record.advancements());
                statement.setDouble(13, record.distanceWalked());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void saveSessions(Connection current, List<ActivityClosedSession> sessions) throws SQLException {
        if (sessions.isEmpty()) {
            return;
        }
        try (PreparedStatement insertSession = current.prepareStatement("""
                INSERT INTO activity_sessions(uuid, name, join_time, quit_time, duration_seconds)
                VALUES (?, ?, ?, ?, ?)
                """);
             PreparedStatement updatePlayer = current.prepareStatement("""
                INSERT INTO players(uuid, name, first_join, last_join, last_seen,
                  total_playtime_seconds, sessions, is_new_player)
                VALUES (?, ?, ?, ?, ?, ?, 1, 0)
                ON CONFLICT(uuid) DO UPDATE SET
                  name=excluded.name,
                  last_seen=excluded.last_seen,
                  total_playtime_seconds=players.total_playtime_seconds + excluded.total_playtime_seconds,
                  sessions=players.sessions + 1
                """)) {
            for (ActivityClosedSession session : sessions) {
                insertSession.setString(1, session.uuid().toString());
                insertSession.setString(2, session.name());
                insertSession.setLong(3, session.joinTime());
                insertSession.setLong(4, session.quitTime());
                insertSession.setLong(5, session.durationSeconds());
                insertSession.addBatch();

                updatePlayer.setString(1, session.uuid().toString());
                updatePlayer.setString(2, session.name());
                updatePlayer.setLong(3, session.joinTime());
                updatePlayer.setLong(4, session.joinTime());
                updatePlayer.setLong(5, session.quitTime());
                updatePlayer.setLong(6, session.durationSeconds());
                updatePlayer.addBatch();
            }
            insertSession.executeBatch();
            updatePlayer.executeBatch();
        }
    }

    private void saveCommands(Connection current, List<ActivityCommandLog> commands) throws SQLException {
        if (commands.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = current.prepareStatement("""
                INSERT INTO activity_commands(uuid, name, command, timestamp)
                VALUES (?, ?, ?, ?)
                """)) {
            for (ActivityCommandLog command : commands) {
                statement.setString(1, command.uuid().toString());
                statement.setString(2, command.name());
                statement.setString(3, command.command());
                statement.setLong(4, command.timestamp());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public synchronized void saveSnapshot(long timestamp, int onlinePlayers, double tps, long memoryUsedMb) {
        if (!available) {
            return;
        }
        execute("""
                INSERT INTO activity_snapshots(timestamp, online_players, tps, memory_used)
                VALUES (?, ?, ?, ?)
                """, statement -> {
            statement.setLong(1, timestamp);
            statement.setInt(2, onlinePlayers);
            statement.setDouble(3, tps);
            statement.setLong(4, memoryUsedMb);
        });
    }

    public synchronized List<ActivityTopEntry> top(String fromDate, String toDate, String column, int limit) {
        if (!List.of("playtime_seconds", "commands_used", "blocks_broken", "deaths", "player_kills", "mob_kills").contains(column)) {
            throw new IllegalArgumentException("Invalid ranking column: " + column);
        }
        List<ActivityTopEntry> entries = new ArrayList<>();
        if (!available) {
            return entries;
        }
        String sql = "SELECT uuid, MAX(name) AS name, SUM(" + column + ") AS value "
                + "FROM activity_daily WHERE date BETWEEN ? AND ? GROUP BY uuid ORDER BY value DESC LIMIT ?";
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, fromDate);
            statement.setString(2, toDate);
            statement.setInt(3, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new ActivityTopEntry(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("name"),
                            resultSet.getLong("value")
                    ));
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao gerar ranking: " + exception.getMessage());
        }
        return entries;
    }

    public synchronized ActivityPeriodSummary summary(String fromDate, String toDate) {
        if (!available) {
            return emptySummary(fromDate, toDate);
        }
        long uniquePlayers = countUniquePlayers(fromDate, toDate);
        long newPlayers = countNewPlayers(fromDate, toDate);
        long totalPlaytime = 0;
        long joins = 0;
        long commands = 0;
        long deaths = 0;
        long playerKills = 0;
        long mobKills = 0;
        long blocksBroken = 0;
        long blocksPlaced = 0;
        long advancements = 0;
        double distance = 0;
        try (PreparedStatement statement = connection().prepareStatement("""
                SELECT
                  COALESCE(SUM(playtime_seconds), 0) AS playtime_seconds,
                  COALESCE(SUM(joins), 0) AS joins,
                  COALESCE(SUM(commands_used), 0) AS commands_used,
                  COALESCE(SUM(deaths), 0) AS deaths,
                  COALESCE(SUM(player_kills), 0) AS player_kills,
                  COALESCE(SUM(mob_kills), 0) AS mob_kills,
                  COALESCE(SUM(blocks_broken), 0) AS blocks_broken,
                  COALESCE(SUM(blocks_placed), 0) AS blocks_placed,
                  COALESCE(SUM(advancements), 0) AS advancements,
                  COALESCE(SUM(distance_walked), 0) AS distance_walked
                FROM activity_daily
                WHERE date BETWEEN ? AND ?
                """)) {
            statement.setString(1, fromDate);
            statement.setString(2, toDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    totalPlaytime = resultSet.getLong("playtime_seconds");
                    joins = resultSet.getLong("joins");
                    commands = resultSet.getLong("commands_used");
                    deaths = resultSet.getLong("deaths");
                    playerKills = resultSet.getLong("player_kills");
                    mobKills = resultSet.getLong("mob_kills");
                    blocksBroken = resultSet.getLong("blocks_broken");
                    blocksPlaced = resultSet.getLong("blocks_placed");
                    advancements = resultSet.getLong("advancements");
                    distance = resultSet.getDouble("distance_walked");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao consultar resumo: " + exception.getMessage());
        }
        SnapshotStats snapshots = snapshotStats(fromDate, toDate);
        return new ActivityPeriodSummary(fromDate, toDate, uniquePlayers, newPlayers, totalPlaytime, joins, commands,
                deaths, playerKills, mobKills, blocksBroken, blocksPlaced, advancements, distance,
                snapshots.peakOnline(), snapshots.averageOnline(), snapshots.busiestHour());
    }

    private long countUniquePlayers(String fromDate, String toDate) {
        try (PreparedStatement statement = connection().prepareStatement("""
                SELECT COUNT(DISTINCT uuid) AS total FROM activity_daily
                WHERE date BETWEEN ? AND ? AND (joins > 0 OR playtime_seconds > 0)
                """)) {
            statement.setString(1, fromDate);
            statement.setString(2, toDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("total") : 0;
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao contar jogadores unicos: " + exception.getMessage());
            return 0;
        }
    }

    private long countNewPlayers(String fromDate, String toDate) {
        long fromEpoch = LocalDate.parse(fromDate).atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond();
        long toEpoch = LocalDate.parse(toDate).plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond() - 1;
        try (PreparedStatement statement = connection().prepareStatement("""
                SELECT COUNT(*) AS total FROM players
                WHERE first_join BETWEEN ? AND ?
                """)) {
            statement.setLong(1, fromEpoch);
            statement.setLong(2, toEpoch);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("total") : 0;
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao contar novos jogadores: " + exception.getMessage());
            return 0;
        }
    }

    private SnapshotStats snapshotStats(String fromDate, String toDate) {
        long fromEpoch = LocalDate.parse(fromDate).atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond();
        long toEpoch = LocalDate.parse(toDate).plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond() - 1;
        int peak = 0;
        double average = 0;
        int busiestHour = -1;
        try (PreparedStatement statement = connection().prepareStatement("""
                SELECT COALESCE(MAX(online_players), 0) AS peak,
                       COALESCE(AVG(online_players), 0) AS average
                FROM activity_snapshots
                WHERE timestamp BETWEEN ? AND ?
                """)) {
            statement.setLong(1, fromEpoch);
            statement.setLong(2, toEpoch);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    peak = resultSet.getInt("peak");
                    average = resultSet.getDouble("average");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao consultar snapshots: " + exception.getMessage());
        }
        try (PreparedStatement statement = connection().prepareStatement("""
                SELECT CAST(strftime('%H', timestamp, 'unixepoch', 'localtime') AS INTEGER) AS hour,
                       AVG(online_players) AS average_online
                FROM activity_snapshots
                WHERE timestamp BETWEEN ? AND ?
                GROUP BY hour
                ORDER BY average_online DESC
                LIMIT 1
                """)) {
            statement.setLong(1, fromEpoch);
            statement.setLong(2, toEpoch);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    busiestHour = resultSet.getInt("hour");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao consultar horario ativo: " + exception.getMessage());
        }
        return new SnapshotStats(peak, average, busiestHour);
    }

    public synchronized Optional<ActivityPlayerProfile> playerProfile(String name, String today) {
        if (!available) {
            return Optional.empty();
        }
        try (PreparedStatement statement = connection().prepareStatement("""
                SELECT p.uuid, p.name, p.first_join, p.last_join, p.last_seen,
                       p.total_playtime_seconds, p.sessions,
                       COALESCE(d.playtime_seconds, 0) AS today_playtime,
                       COALESCE(d.commands_used, 0) AS today_commands,
                       COALESCE(d.deaths, 0) AS today_deaths,
                       COALESCE(d.blocks_broken, 0) AS today_blocks
                FROM players p
                LEFT JOIN activity_daily d ON d.uuid = p.uuid AND d.date = ?
                WHERE lower(p.name) = lower(?)
                LIMIT 1
                """)) {
            statement.setString(1, today);
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new ActivityPlayerProfile(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("name"),
                            resultSet.getLong("first_join"),
                            resultSet.getLong("last_join"),
                            resultSet.getLong("last_seen"),
                            resultSet.getLong("total_playtime_seconds"),
                            resultSet.getLong("sessions"),
                            resultSet.getLong("today_playtime"),
                            resultSet.getLong("today_commands"),
                            resultSet.getLong("today_deaths"),
                            resultSet.getLong("today_blocks")
                    ));
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao consultar perfil: " + exception.getMessage());
        }
        return Optional.empty();
    }

    public synchronized int peakForDate(String date) {
        if (!available) {
            return 0;
        }
        SnapshotStats stats = snapshotStats(date, date);
        return stats.peakOnline();
    }

    public synchronized long latestActivityTimestamp() {
        if (!available) {
            return 0;
        }
        try (Statement statement = connection().createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT MAX(value) AS latest FROM (
                       SELECT MAX(last_seen) AS value FROM players
                       UNION ALL
                       SELECT MAX(timestamp) AS value FROM activity_snapshots WHERE online_players > 0
                     )
                     """)) {
            return resultSet.next() ? resultSet.getLong("latest") : 0;
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao consultar ultima atividade: " + exception.getMessage());
            return 0;
        }
    }

    public synchronized String latestPlayerName() {
        if (!available) {
            return "desconhecido";
        }
        try (Statement statement = connection().createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT name FROM players ORDER BY last_seen DESC LIMIT 1
                     """)) {
            return resultSet.next() ? resultSet.getString("name") : "desconhecido";
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao consultar ultimo jogador: " + exception.getMessage());
            return "desconhecido";
        }
    }

    public synchronized String busiestDay(String fromDate, String toDate) {
        if (!available) {
            return "";
        }
        try (PreparedStatement statement = connection().prepareStatement("""
                SELECT date, SUM(playtime_seconds) AS activity
                FROM activity_daily
                WHERE date BETWEEN ? AND ?
                GROUP BY date
                ORDER BY activity DESC
                LIMIT 1
                """)) {
            statement.setString(1, fromDate);
            statement.setString(2, toDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("date") : "";
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao consultar dia ativo: " + exception.getMessage());
            return "";
        }
    }

    public synchronized double newPlayerRetention(String fromDate, String toDate, long minimumSeconds) {
        if (!available) {
            return 0;
        }
        long fromEpoch = LocalDate.parse(fromDate).atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond();
        long toEpoch = LocalDate.parse(toDate).plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond() - 1;
        try (PreparedStatement statement = connection().prepareStatement("""
                SELECT COUNT(*) AS total,
                       SUM(CASE WHEN total_playtime_seconds >= ? THEN 1 ELSE 0 END) AS retained
                FROM players
                WHERE first_join BETWEEN ? AND ?
                """)) {
            statement.setLong(1, minimumSeconds);
            statement.setLong(2, fromEpoch);
            statement.setLong(3, toEpoch);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return 0;
                }
                long total = resultSet.getLong("total");
                return total == 0 ? 0 : resultSet.getLong("retained") * 100.0 / total;
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro ao calcular retencao: " + exception.getMessage());
            return 0;
        }
    }

    private ActivityPeriodSummary emptySummary(String fromDate, String toDate) {
        return new ActivityPeriodSummary(fromDate, toDate, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1);
    }

    private void execute(String sql, SqlBinder binder) {
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("[HaizActivity] Erro SQLite: " + exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private record SnapshotStats(int peakOnline, double averageOnline, int busiestHour) {
    }
}
