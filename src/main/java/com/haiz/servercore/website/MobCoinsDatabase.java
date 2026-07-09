package com.haiz.servercore.website;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

public final class MobCoinsDatabase {

    private final JavaPlugin plugin;
    private String dbPath;
    private Connection connection;

    public MobCoinsDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
        findDatabase();
    }

    private void findDatabase() {
        // Try to find IridiumMobCoins.db in the plugins folder
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        File mobcoinsDb = new File(pluginsFolder, "IridiumMobCoins/IridiumMobCoins.db");

        if (mobcoinsDb.exists()) {
            this.dbPath = mobcoinsDb.getAbsolutePath();
            plugin.getLogger().info("[MobCoins] Database encontrada: " + dbPath);
        } else {
            plugin.getLogger().warning("[MobCoins] Database não encontrada em: " + mobcoinsDb.getAbsolutePath());
            // Try alternative path
            File altPath = new File(pluginsFolder, "IridiumMobCoins.db");
            if (altPath.exists()) {
                this.dbPath = altPath.getAbsolutePath();
                plugin.getLogger().info("[MobCoins] Database encontrada (alt): " + dbPath);
            } else {
                plugin.getLogger().warning("[MobCoins] Nenhuma database encontrada. Saldo sempre será 0.");
            }
        }
    }

    private Connection getConnection() throws Exception {
        if (connection == null || connection.isClosed()) {
            if (dbPath == null) {
                throw new Exception("Database path not configured");
            }
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            connection.setAutoCommit(true);
        }
        return connection;
    }

    public double getBalance(String playerName) {
        if (dbPath == null) return 0;

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT mobcoins FROM users WHERE name = ? COLLATE NOCASE"
            );
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("mobcoins");
                plugin.getLogger().info("[MobCoins] Saldo de " + playerName + ": " + balance);
                return balance;
            }

            plugin.getLogger().info("[MobCoins] Jogador não encontrado: " + playerName);
            return 0;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[MobCoins] Erro ao ler saldo: " + e.getMessage());
            return 0;
        }
    }

    public double getBalance(java.util.UUID uuid) {
        if (dbPath == null) return 0;

        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT mobcoins FROM users WHERE uuid = ?"
            );
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("mobcoins");
            }
            return 0;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[MobCoins] Erro ao ler saldo por UUID: " + e.getMessage());
            return 0;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}
