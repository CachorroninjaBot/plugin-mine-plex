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
        plugin.getLogger().info("[MobCoins] Procurando database em: " + pluginsFolder.getAbsolutePath());

        // List all files in plugins folder for debugging
        File[] pluginFolders = pluginsFolder.listFiles();
        if (pluginFolders != null) {
            for (File f : pluginFolders) {
                if (f.isDirectory() && f.getName().toLowerCase().contains("iridium")) {
                    plugin.getLogger().info("[MobCoins] Encontrou pasta: " + f.getName());
                    File[] files = f.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            plugin.getLogger().info("[MobCoins]   - " + file.getName() + " (" + file.length() + " bytes)");
                        }
                    }
                }
            }
        }

        File mobcoinsDb = new File(pluginsFolder, "IridiumMobCoins/IridiumMobCoins.db");
        plugin.getLogger().info("[MobCoins] Tentando: " + mobcoinsDb.getAbsolutePath() + " (existe: " + mobcoinsDb.exists() + ")");

        if (mobcoinsDb.exists()) {
            this.dbPath = mobcoinsDb.getAbsolutePath();
            plugin.getLogger().info("[MobCoins] ✅ Database encontrada: " + dbPath);
        } else {
            plugin.getLogger().warning("[MobCoins] ❌ Database não encontrada em: " + mobcoinsDb.getAbsolutePath());
            // Try alternative paths
            File altPath1 = new File(pluginsFolder, "IridiumMobCoins.db");
            File altPath2 = new File(pluginsFolder, "iridiummobcoins/IridiumMobCoins.db");

            plugin.getLogger().info("[MobCoins] Tentando alt1: " + altPath1.getAbsolutePath() + " (existe: " + altPath1.exists() + ")");
            plugin.getLogger().info("[MobCoins] Tentando alt2: " + altPath2.getAbsolutePath() + " (existe: " + altPath2.exists() + ")");

            if (altPath1.exists()) {
                this.dbPath = altPath1.getAbsolutePath();
                plugin.getLogger().info("[MobCoins] ✅ Database encontrada (alt1): " + dbPath);
            } else if (altPath2.exists()) {
                this.dbPath = altPath2.getAbsolutePath();
                plugin.getLogger().info("[MobCoins] ✅ Database encontrada (alt2): " + dbPath);
            } else {
                plugin.getLogger().severe("[MobCoins] ❌ Nenhuma database encontrada! Saldo sempre será 0.");
                plugin.getLogger().severe("[MobCoins] Verifique se o plugin IridiumMobCoins está instalado corretamente.");
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
