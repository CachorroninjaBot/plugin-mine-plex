package com.haiz.servercore;

import com.haiz.servercore.commands.HaizCoreCommand;
import com.haiz.servercore.config.ConfigManager;
import com.haiz.servercore.discord.DiscordBotManager;
import com.haiz.servercore.discord.ServerStatusNotifier;
import com.haiz.servercore.storage.SQLiteDatabase;
import com.haiz.servercore.update.UpdateManager;
import com.haiz.servercore.vip.VipModule;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class HaizServerCore extends JavaPlugin {
    private ConfigManager configManager;
    private SQLiteDatabase sqliteDatabase;
    private DiscordBotManager discordBotManager;
    private ServerStatusNotifier statusNotifier;
    private VipModule vipModule;
    private UpdateManager updateManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.updateManager = new UpdateManager(this,
                getConfig().getString("updater.github-owner", "HaizMC"),
                getConfig().getString("updater.github-repo", "HaizServerCore"),
                getConfig().getInt("updater.check-interval-minutes", 30));

        if (updateManager.applyPendingUpdate()) return;

        updateManager.mergeConfig();

        this.configManager = new ConfigManager(this);
        this.configManager.reload();

        this.discordBotManager = new DiscordBotManager(this);
        this.discordBotManager.start();

        this.sqliteDatabase = new SQLiteDatabase(this);

        this.statusNotifier = new ServerStatusNotifier(this,
                getConfig().getString("discord.status-webhook", ""));
        Bukkit.getScheduler().runTaskLater(this, () -> statusNotifier.sendOnlineStatus(), 60L);

        this.vipModule = new VipModule(this);
        this.vipModule.start();

        this.updateManager.start();

        PluginCommand command = getCommand("haizcore");
        if (command != null) {
            HaizCoreCommand executor = new HaizCoreCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("HaizServerCore habilitado. Discord=" + discordBotManager.getStateLabel() + " Versão=" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        statusNotifier.sendOfflineStatus();

        if (vipModule != null) {
            vipModule.stop();
        }
        if (updateManager != null) {
            updateManager.stop();
        }
        if (discordBotManager != null) {
            discordBotManager.stop();
        }
        if (sqliteDatabase != null) {
            sqliteDatabase.close();
        }
        getLogger().info("HaizServerCore desabilitado.");
    }

    public void reloadEverything() {
        if (updateManager != null && updateManager.isUpdateDownloaded()) {
            updateManager.applyPendingUpdate();
            return;
        }

        reloadConfig();
        configManager.reload();
        if (discordBotManager != null) {
            discordBotManager.reload();
        }
        if (vipModule != null) {
            vipModule.reload();
        }
        if (updateManager != null) {
            updateManager.stop();
            updateManager = new UpdateManager(this,
                    getConfig().getString("updater.github-owner", "HaizMC"),
                    getConfig().getString("updater.github-repo", "HaizServerCore"),
                    getConfig().getInt("updater.check-interval-minutes", 30));
            updateManager.start();
        }
    }

    public ConfigManager config() { return configManager; }
    public DiscordBotManager discord() { return discordBotManager; }
    public VipModule vip() { return vipModule; }
    public SQLiteDatabase sqliteDatabase() { return sqliteDatabase; }
    public UpdateManager updateManager() { return updateManager; }
}
