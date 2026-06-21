package com.haiz.servercore;

import com.haiz.servercore.activity.HaizActivityModule;
import com.haiz.servercore.commands.HaizCoreCommand;
import com.haiz.servercore.config.ConfigManager;
import com.haiz.servercore.config.MessagesManager;
import com.haiz.servercore.discord.DiscordBotManager;
import com.haiz.servercore.discord.DiscordEmbedFactory;
import com.haiz.servercore.discord.DiscordLogService;
import com.haiz.servercore.logs.ConsoleOutputAppender;
import com.haiz.servercore.logs.LogManager;
import com.haiz.servercore.metrics.MetricsManager;
import com.haiz.servercore.storage.DatabaseManager;
import com.haiz.servercore.vip.VipModule;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class HaizServerCore extends JavaPlugin {
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private DatabaseManager databaseManager;
    private MetricsManager metricsManager;
    private DiscordBotManager discordBotManager;
    private DiscordLogService discordLogService;
    private LogManager logManager;
    private ConsoleOutputAppender consoleOutputAppender;
    private HaizActivityModule activityModule;
    private VipModule vipModule;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.configManager = new ConfigManager(this);
        this.messagesManager = new MessagesManager(this);
        this.configManager.reload();
        this.messagesManager.reload();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.start();

        this.discordLogService = new DiscordLogService(this);
        this.discordBotManager = new DiscordBotManager(this, discordLogService);
        this.metricsManager = new MetricsManager(this, databaseManager, discordLogService);
        this.logManager = new LogManager(this, databaseManager, metricsManager, discordLogService);
        this.activityModule = new HaizActivityModule(this);

        this.metricsManager.start();
        this.logManager.register();
        this.activityModule.start();
        this.discordBotManager.start();
        
        // Inicialização do Módulo VIP após o Discord
        this.vipModule = new VipModule(this);
        this.vipModule.start();

        this.discordLogService.joinLeave(DiscordEmbedFactory.serverStarted(getServer().getOnlinePlayers().size()));

        if (configManager.isConsoleOutputCaptureEnabled()) {
            this.consoleOutputAppender = new ConsoleOutputAppender(this, discordLogService);
            this.consoleOutputAppender.startCapture();
        }

        PluginCommand command = getCommand("haizcore");
        if (command != null) {
            HaizCoreCommand executor = new HaizCoreCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("HaizServerCore habilitado. Discord=" + discordBotManager.getStateLabel()
                + ", database=" + databaseManager.getStateLabel()
                + ", metrics=" + (configManager.isMetricsEnabled() ? "on" : "off"));
    }

    @Override
    public void onDisable() {
        if (vipModule != null) {
            vipModule.stop();
        }
        if (consoleOutputAppender != null) {
            consoleOutputAppender.stopCapture();
        }
        if (logManager != null) {
            logManager.unregister();
        }
        if (metricsManager != null) {
            metricsManager.stop();
        }
        if (activityModule != null) {
            activityModule.stop();
        }
        if (discordBotManager != null) {
            if (discordLogService != null) {
                discordLogService.joinLeaveNow(DiscordEmbedFactory.serverStopped(getServer().getOnlinePlayers().size()));
            }
            if (vipModule != null) {
                vipModule.stop();
            }
            discordBotManager.stop();
        }
        if (discordLogService != null) {
            discordLogService.stop();
        }
        if (databaseManager != null) {
            databaseManager.stop();
        }
        getLogger().info("HaizServerCore desabilitado.");
    }

    public VipModule vip() { return vipModule; }

    public void reloadEverything() {
        reloadConfig();
        configManager.reload();
        messagesManager.reload();
        if (consoleOutputAppender != null) {
            consoleOutputAppender.stopCapture();
            consoleOutputAppender = null;
        }
        if (configManager.isConsoleOutputCaptureEnabled()) {
            consoleOutputAppender = new ConsoleOutputAppender(this, discordLogService);
            consoleOutputAppender.startCapture();
        }
        discordBotManager.reload();
        metricsManager.reload();
        logManager.reload();
        activityModule.reload();
        
        if (vipModule != null) {
            vipModule.reload();
        }
    }

    public ConfigManager config() {
        return configManager;
    }

    public MessagesManager messages() {
        return messagesManager;
    }

    public DatabaseManager database() {
        return databaseManager;
    }

    public MetricsManager metrics() {
        return metricsManager;
    }

    public DiscordBotManager discord() {
        return discordBotManager;
    }

    public DiscordLogService discordLogs() {
        return discordLogService;
    }

    public HaizActivityModule activity() {
        return activityModule;
    }
}
