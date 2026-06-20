package com.haiz.servercore.storage;

import org.bukkit.plugin.java.JavaPlugin;

public final class DatabaseManager {
    private final JavaPlugin plugin;
    private final SQLiteDatabase sqliteDatabase;
    private final PlayerStatsRepository playerStatsRepository;
    private final ServerStatsRepository serverStatsRepository;
    private volatile boolean available;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.sqliteDatabase = new SQLiteDatabase(plugin);
        this.playerStatsRepository = new PlayerStatsRepository(plugin, sqliteDatabase);
        this.serverStatsRepository = new ServerStatsRepository(plugin, sqliteDatabase);
    }

    public void start() {
        try {
            sqliteDatabase.open();
            available = true;
            plugin.getLogger().info("SQLite inicializado com sucesso.");
        } catch (Exception exception) {
            available = false;
            plugin.getLogger().severe(
                    "SQLite falhou ao iniciar. O plugin continuara sem persistencia: " + exception.getMessage());
        }
    }

    public void stop() {
        sqliteDatabase.close();
        available = false;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getStateLabel() {
        return available ? "online" : "offline";
    }

    public SQLiteDatabase sqliteDatabase() {
        return sqliteDatabase;
    }

    public PlayerStatsRepository players() {
        return playerStatsRepository;
    }

    public ServerStatsRepository serverStats() {
        return serverStatsRepository;
    }
}
