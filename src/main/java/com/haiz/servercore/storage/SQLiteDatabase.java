package com.haiz.servercore.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class SQLiteDatabase {
    private final JavaPlugin plugin;
    private Connection connection;

    public SQLiteDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void open() throws SQLException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Nao foi possivel criar a pasta de dados do plugin.");
        }
        String fileName = plugin.getConfig().getString("storage.sqlite.file", "haizservercore.db");
        File file = new File(plugin.getDataFolder(), fileName);
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA foreign_keys=ON");
        }
        createTables();
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS players (
                      uuid TEXT PRIMARY KEY,
                      name TEXT,
                      first_join INTEGER,
                      last_join INTEGER,
                      last_quit INTEGER,
                      total_playtime_seconds INTEGER DEFAULT 0,
                      total_sessions INTEGER DEFAULT 0,
                      total_commands INTEGER DEFAULT 0,
                      total_chat_messages INTEGER DEFAULT 0,
                      total_deaths INTEGER DEFAULT 0,
                      total_kills INTEGER DEFAULT 0,
                      blocks_broken INTEGER DEFAULT 0,
                      blocks_placed INTEGER DEFAULT 0,
                      distance_walked DOUBLE DEFAULT 0
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS sessions (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      uuid TEXT,
                      join_time INTEGER,
                      quit_time INTEGER,
                      duration_seconds INTEGER,
                      afk_seconds INTEGER
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS command_logs (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      executor TEXT,
                      executor_type TEXT,
                      command TEXT,
                      world TEXT,
                      x DOUBLE,
                      y DOUBLE,
                      z DOUBLE,
                      created_at INTEGER
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS server_snapshots (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      online_players INTEGER,
                      tps DOUBLE,
                      mspt DOUBLE,
                      unique_players_today INTEGER,
                      loaded_chunks INTEGER DEFAULT 0,
                      entities INTEGER DEFAULT 0,
                      used_memory_mb INTEGER DEFAULT 0,
                      max_memory_mb INTEGER DEFAULT 0,
                      active_players INTEGER DEFAULT 0,
                      afk_players INTEGER DEFAULT 0,
                      avg_ping DOUBLE DEFAULT 0,
                      created_at INTEGER
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS daily_stats (
                      day TEXT PRIMARY KEY,
                      unique_players INTEGER,
                      new_players INTEGER,
                      peak_online INTEGER,
                      total_playtime_seconds INTEGER,
                      total_sessions INTEGER,
                      avg_session_seconds INTEGER,
                      commands_executed INTEGER,
                      chat_messages INTEGER,
                      deaths INTEGER DEFAULT 0,
                      kills INTEGER DEFAULT 0,
                      blocks_broken INTEGER DEFAULT 0,
                      blocks_placed INTEGER DEFAULT 0,
                      distance_walked DOUBLE DEFAULT 0,
                      avg_tps DOUBLE DEFAULT -1,
                      min_tps DOUBLE DEFAULT -1,
                      max_mspt DOUBLE DEFAULT 0
                    )
                    """);
            migrateColumns(statement);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sessions_uuid ON sessions(uuid)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_command_logs_created ON command_logs(created_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_snapshots_created ON server_snapshots(created_at)");
        }
    }

    private void migrateColumns(Statement statement) {
        addColumn(statement, "server_snapshots", "loaded_chunks INTEGER DEFAULT 0");
        addColumn(statement, "server_snapshots", "entities INTEGER DEFAULT 0");
        addColumn(statement, "server_snapshots", "used_memory_mb INTEGER DEFAULT 0");
        addColumn(statement, "server_snapshots", "max_memory_mb INTEGER DEFAULT 0");
        addColumn(statement, "server_snapshots", "active_players INTEGER DEFAULT 0");
        addColumn(statement, "server_snapshots", "afk_players INTEGER DEFAULT 0");
        addColumn(statement, "server_snapshots", "avg_ping DOUBLE DEFAULT 0");
        addColumn(statement, "daily_stats", "deaths INTEGER DEFAULT 0");
        addColumn(statement, "daily_stats", "kills INTEGER DEFAULT 0");
        addColumn(statement, "daily_stats", "blocks_broken INTEGER DEFAULT 0");
        addColumn(statement, "daily_stats", "blocks_placed INTEGER DEFAULT 0");
        addColumn(statement, "daily_stats", "distance_walked DOUBLE DEFAULT 0");
        addColumn(statement, "daily_stats", "avg_tps DOUBLE DEFAULT -1");
        addColumn(statement, "daily_stats", "min_tps DOUBLE DEFAULT -1");
        addColumn(statement, "daily_stats", "max_mspt DOUBLE DEFAULT 0");
    }

    private void addColumn(Statement statement, String table, String columnDefinition) {
        try {
            statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + columnDefinition);
        } catch (SQLException ignored) {
            // Column already exists.
        }
    }

    public synchronized Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            open();
        }
        return connection;
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Erro ao fechar SQLite: " + exception.getMessage());
        }
    }
}
