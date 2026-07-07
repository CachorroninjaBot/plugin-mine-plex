package com.haiz.servercore.storage;

import com.haiz.servercore.HaizServerCore;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        String fileName = "haizservercore.db";
        if (plugin instanceof HaizServerCore core) {
            fileName = core.config().getModuleConfig("storage").getString("sqlite.file", "haizservercore.db");
        }
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

    public String getDiscordIdByUuid(String uuid) {
        try {
            Connection conn = connection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT discord_id FROM discord_links WHERE uuid = ?")) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("discord_id");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[SQLite] Falha ao buscar discord_id: " + e.getMessage());
        }
        return null;
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
