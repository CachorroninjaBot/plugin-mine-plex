package com.haiz.servercore;

import com.haiz.servercore.commands.HaizCoreCommand;
import com.haiz.servercore.config.ConfigManager;
import com.haiz.servercore.discord.DiscordBotManager;
import com.haiz.servercore.discord.ServerStatusNotifier;
import com.haiz.servercore.discord.chat.ChatBridgeListener;
import com.haiz.servercore.discord.chat.GameChatListener;
import com.haiz.servercore.discord.commandlog.CommandLogModule;
import com.haiz.servercore.discord.console.ConsoleChannelManager;
import com.haiz.servercore.discord.events.PlayerAdvancementListener;
import com.haiz.servercore.discord.events.PlayerDeathListener;
import com.haiz.servercore.discord.events.PlayerJoinLeaveListener;
import com.haiz.servercore.discord.responses.CannedResponseManager;
import com.haiz.servercore.discord.sync.GroupRoleSyncManager;
import com.haiz.servercore.discord.sync.NicknameSyncManager;
import com.haiz.servercore.storage.SQLiteDatabase;
import com.haiz.servercore.teams.TeamsModule;
import com.haiz.servercore.vip.VipModule;
import com.haiz.servercore.website.WebsiteModule;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class HaizServerCore extends JavaPlugin {
    private ConfigManager configManager;
    private SQLiteDatabase sqliteDatabase;
    private DiscordBotManager discordBotManager;
    private ServerStatusNotifier statusNotifier;
    private VipModule vipModule;
    private TeamsModule teamsModule;
    private CommandLogModule commandLogModule;
    private WebsiteModule websiteModule;

    private ChatBridgeListener chatBridgeListener;
    private GameChatListener gameChatListener;
    private PlayerJoinLeaveListener joinLeaveListener;
    private PlayerDeathListener deathListener;
    private PlayerAdvancementListener advancementListener;
    private GroupRoleSyncManager groupRoleSyncManager;
    private NicknameSyncManager nicknameSyncManager;
    private ConsoleChannelManager consoleChannelManager;
    private CannedResponseManager cannedResponseManager;
    private long serverStartTime;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.serverStartTime = System.currentTimeMillis();

        this.configManager = new ConfigManager(this);
        this.configManager.reload();

        this.discordBotManager = new DiscordBotManager(this);
        this.discordBotManager.start();

        this.sqliteDatabase = new SQLiteDatabase(this);

        this.statusNotifier = new ServerStatusNotifier(this,
                configManager.statusWebhookUrl());
        Bukkit.getScheduler().runTaskLater(this, () -> statusNotifier.sendOnlineStatus(), 60L);

        this.vipModule = new VipModule(this);
        this.vipModule.start();

        this.teamsModule = new TeamsModule(this);
        this.teamsModule.start();

        this.commandLogModule = new CommandLogModule(this);

        this.websiteModule = new WebsiteModule(this);
        this.websiteModule.start();

        Bukkit.getScheduler().runTaskLater(this, this::startDiscordModules, 40L);

        PluginCommand command = getCommand("haizcore");
        if (command != null) {
            HaizCoreCommand executor = new HaizCoreCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("HaizServerCore habilitado. Discord=" + discordBotManager.getStateLabel() + " Versão=" + getDescription().getVersion());
    }

    private void startDiscordModules() {
        if (!discordBotManager.isOnline()) {
            getLogger().warning("[Discord] Bot não está online. Módulos Discord não iniciados.");
            return;
        }

        if (configManager.isChatBridgeEnabled()) {
            chatBridgeListener = new ChatBridgeListener(this);
            discordBotManager.addListener(chatBridgeListener);

            if (configManager.isMinecraftToDiscordEnabled()) {
                gameChatListener = new GameChatListener(this);
                Bukkit.getPluginManager().registerEvents(gameChatListener, this);
            }
            getLogger().info("[ChatBridge] Módulo iniciado.");
        }

        if (configManager.isEventsEnabled()) {
            joinLeaveListener = new PlayerJoinLeaveListener(this);
            Bukkit.getPluginManager().registerEvents(joinLeaveListener, this);

            if (configManager.isEventDeathEnabled()) {
                deathListener = new PlayerDeathListener(this);
                Bukkit.getPluginManager().registerEvents(deathListener, this);
            }

            if (configManager.isEventAdvancementEnabled()) {
                advancementListener = new PlayerAdvancementListener(this);
                Bukkit.getPluginManager().registerEvents(advancementListener, this);
            }
            getLogger().info("[Events] Módulo iniciado.");
        }

        if (configManager.isGroupRoleSyncEnabled()) {
            groupRoleSyncManager = new GroupRoleSyncManager(this);
            groupRoleSyncManager.start();
        }

        if (configManager.isNicknameSyncEnabled()) {
            nicknameSyncManager = new NicknameSyncManager(this);
            nicknameSyncManager.start();
        }

        if (configManager.isConsoleEnabled()) {
            consoleChannelManager = new ConsoleChannelManager(this);
            consoleChannelManager.start();
        }

        if (configManager.isCannedResponsesEnabled()) {
            cannedResponseManager = new CannedResponseManager(this);
            cannedResponseManager.start();
        }

        if (commandLogModule != null) {
            commandLogModule.start();
        }

        registerAllSlashCommands();
    }

    private void registerAllSlashCommands() {
        if (!discordBotManager.isOnline()) return;
        var jda = discordBotManager.jda();
        String guildId = configManager.guildId();

        List<SlashCommandData> allCommands = new ArrayList<>();

        if (vipModule != null && vipModule.isRunning()) {
            allCommands.addAll(vipModule.getSlashCommands());
        }
        if (teamsModule != null && teamsModule.isRunning()) {
            allCommands.addAll(teamsModule.getSlashCommands());
        }

        if (allCommands.isEmpty()) return;

        if (guildId != null && !guildId.isBlank()) {
            Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands().addCommands(allCommands).queue(
                    success -> getLogger().info("[Discord] Comandos slash registrados na guild: " + guild.getName() + " (" + allCommands.size() + " comandos)"),
                    error -> getLogger().warning("[Discord] Falha ao registrar comandos slash: " + error.getMessage())
                );
                return;
            }
            getLogger().warning("[Discord] Guild ID " + guildId + " não encontrada. Registrando globalmente.");
        }

        jda.updateCommands().addCommands(allCommands).queue(
            success -> getLogger().info("[Discord] Comandos slash registrados globalmente (" + allCommands.size() + " comandos)"),
            error -> getLogger().warning("[Discord] Falha ao registrar comandos slash: " + error.getMessage())
        );
    }

    private void stopDiscordModules() {
        if (chatBridgeListener != null) {
            discordBotManager.removeListener(chatBridgeListener);
            chatBridgeListener = null;
        }
        if (gameChatListener != null) {
            gameChatListener = null;
        }
        if (joinLeaveListener != null) {
            joinLeaveListener = null;
        }
        if (deathListener != null) {
            deathListener = null;
        }
        if (advancementListener != null) {
            advancementListener = null;
        }
        if (groupRoleSyncManager != null) {
            groupRoleSyncManager.stop();
            groupRoleSyncManager = null;
        }
        if (nicknameSyncManager != null) {
            nicknameSyncManager.stop();
            nicknameSyncManager = null;
        }
        if (consoleChannelManager != null) {
            consoleChannelManager.stop();
            consoleChannelManager = null;
        }
        if (cannedResponseManager != null) {
            cannedResponseManager.stop();
            cannedResponseManager = null;
        }
    }

    @Override
    public void onDisable() {
        statusNotifier.sendOfflineStatus();

        stopDiscordModules();

        if (vipModule != null) {
            vipModule.stop();
        }
        if (teamsModule != null) {
            teamsModule.stop();
        }
        if (commandLogModule != null) {
            commandLogModule.stop();
        }
        if (websiteModule != null) {
            websiteModule.stop();
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
        stopDiscordModules();
        reloadConfig();
        configManager.reload();
        if (discordBotManager != null) {
            discordBotManager.reload();
        }
        if (vipModule != null) {
            vipModule.reload();
        }
        if (teamsModule != null) {
            teamsModule.reload();
        }
        if (commandLogModule != null) {
            commandLogModule.reload();
        }
        if (websiteModule != null) {
            websiteModule.reload();
        }
        Bukkit.getScheduler().runTaskLater(this, this::startDiscordModules, 40L);
    }

    public ConfigManager config() { return configManager; }
    public DiscordBotManager discord() { return discordBotManager; }
    public VipModule vip() { return vipModule; }
    public TeamsModule teams() { return teamsModule; }
    public CommandLogModule commandLog() { return commandLogModule; }
    public WebsiteModule website() { return websiteModule; }
    public SQLiteDatabase sqliteDatabase() { return sqliteDatabase; }
    public GroupRoleSyncManager groupRoleSyncManager() { return groupRoleSyncManager; }
    public NicknameSyncManager nicknameSyncManager() { return nicknameSyncManager; }
    public long getServerStartTime() { return serverStartTime; }
}
