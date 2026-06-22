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
        try {
            open();
        } catch (SQLException e) {
            plugin.getLogger().warning("Falha ao abrir SQLite: " + e.getMessage());
        }
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
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute("PRAGMA cache_size=-64000");
            statement.execute("PRAGMA temp_store=MEMORY");
        }
    }

    public synchronized Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            plugin.getLogger().warning("SQLite connection estava fechada; reconectando...");
            open();
        }
        return connection;
    }

    public synchronized void close() {
        if (connection == null) return;
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Erro ao fechar SQLite: " + exception.getMessage());
        } finally {
            connection = null;
        }
    }
}
