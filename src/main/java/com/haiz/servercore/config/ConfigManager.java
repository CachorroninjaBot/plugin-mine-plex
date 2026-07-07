package com.haiz.servercore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        configs.clear();
        plugin.reloadConfig();
    }

    public FileConfiguration getModuleConfig(String name) {
        return configs.computeIfAbsent(name, this::loadConfig);
    }

    private FileConfiguration loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), "config/" + name + ".yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = plugin.getResource("config/" + name + ".yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    // ── Discord Bot ──────────────────────────────────────────────

    public boolean isDiscordEnabled() {
        return getModuleConfig("discord").getBoolean("enabled", true);
    }

    public String discordToken() {
        return getModuleConfig("discord").getString("token", "");
    }

    public String guildId() {
        return getModuleConfig("discord").getString("guild-id", "");
    }

    public boolean isActivityEnabled() {
        return getModuleConfig("discord").getBoolean("activity.enabled", true);
    }

    public String activityText() {
        return getModuleConfig("discord").getString("activity.text", "Monitorando o servidor");
    }

    public String activityType() {
        return getModuleConfig("discord").getString("activity.type", "PLAYING");
    }

    // ── Chat Bridge ──────────────────────────────────────────────

    public boolean isChatBridgeEnabled() {
        return getModuleConfig("chat").getBoolean("enabled", false);
    }

    public boolean isDiscordToMinecraftEnabled() {
        return getModuleConfig("chat").getBoolean("discord-to-minecraft.enabled", false);
    }

    public List<String> chatBridgeChannelIds() {
        return getModuleConfig("chat").getStringList("discord-to-minecraft.channel-ids");
    }

    public String discordToMinecraftFormat() {
        return getModuleConfig("chat").getString("discord-to-minecraft.format", "&b[Discord] &e%name% &7>> &f%message%");
    }

    public boolean chatBridgeBlockBots() {
        return getModuleConfig("chat").getBoolean("discord-to-minecraft.block-bots", true);
    }

    public String chatBridgeEmojiBehavior() {
        return getModuleConfig("chat").getString("discord-to-minecraft.emoji-behavior", "show");
    }

    public String chatBridgeMentionBehavior() {
        return getModuleConfig("chat").getString("discord-to-minecraft.mention-behavior", "show");
    }

    public int chatBridgeMaxLength() {
        return getModuleConfig("chat").getInt("discord-to-minecraft.max-length", 256);
    }

    public boolean isMinecraftToDiscordEnabled() {
        return getModuleConfig("chat").getBoolean("minecraft-to-discord.enabled", false);
    }

    public String minecraftToDiscordChannelId() {
        return getModuleConfig("chat").getString("minecraft-to-discord.channel-id", "");
    }

    public String minecraftToDiscordFormat() {
        return getModuleConfig("chat").getString("minecraft-to-discord.format", "**%primarygroup%** %displayname% >> %message%");
    }

    public String minecraftToDiscordPrefix() {
        return getModuleConfig("chat").getString("minecraft-to-discord.prefix-required", "");
    }

    public boolean isChatBridgeWebhookEnabled() {
        return getModuleConfig("chat").getBoolean("minecraft-to-discord.use-webhook", false);
    }

    public String chatBridgeWebhookAvatar() {
        return getModuleConfig("chat").getString("minecraft-to-discord.webhook-avatar", "https://mc-heads.net/avatar/%uuid%");
    }

    public String chatBridgeWebhookName() {
        return getModuleConfig("chat").getString("minecraft-to-discord.webhook-name", "%displayname%");
    }

    public boolean isReserializerToDiscord() {
        return getModuleConfig("chat").getBoolean("reserializer.to-discord", true);
    }

    public boolean isReserializerToMinecraft() {
        return getModuleConfig("chat").getBoolean("reserializer.to-minecraft", true);
    }

    // ── Account Linking ──────────────────────────────────────────

    public boolean isAccountLinkingEnabled() {
        return getModuleConfig("linking").getBoolean("enabled", true);
    }

    public int accountLinkingCodeLength() {
        return getModuleConfig("linking").getInt("code-length", 4);
    }

    public int accountLinkingCodeExpiry() {
        return getModuleConfig("linking").getInt("code-expiry-seconds", 120);
    }

    public String accountLinkingRoleId() {
        return getModuleConfig("linking").getString("linked-role-id", "");
    }

    public List<String> accountLinkingOnLinkCommands() {
        return getModuleConfig("linking").getStringList("on-link-commands");
    }

    public List<String> accountLinkingOnUnlinkCommands() {
        return getModuleConfig("linking").getStringList("on-unlink-commands");
    }

    // ── Events ───────────────────────────────────────────────────

    public boolean isEventsEnabled() {
        return getModuleConfig("events").getBoolean("enabled", false);
    }

    public String eventsChannelId() {
        return getModuleConfig("events").getString("channel-id", "");
    }

    public boolean isEventsWebhookEnabled() {
        return getModuleConfig("events").getBoolean("use-webhook", false);
    }

    public String eventsWebhookAvatar() {
        return getModuleConfig("events").getString("webhook-avatar", "https://mc-heads.net/avatar/%uuid%");
    }

    public boolean isEventJoinEnabled() {
        return getModuleConfig("events").getBoolean("join.enabled", true);
    }

    public String eventJoinFormat() {
        return getModuleConfig("events").getString("join.format", "**%displayname%** entrou no servidor.");
    }

    public String eventJoinColor() {
        return getModuleConfig("events").getString("join.embed-color", "#55FF55");
    }

    public String eventJoinThumbnail() {
        return getModuleConfig("events").getString("join.embed-thumbnail", "https://mc-heads.net/avatar/%uuid%");
    }

    public boolean isFirstJoinEnabled() {
        return getModuleConfig("events").getBoolean("first-join.enabled", true);
    }

    public String eventFirstJoinFormat() {
        return getModuleConfig("events").getString("first-join.format", "**%displayname%** entrou pela primeira vez!");
    }

    public String eventFirstJoinColor() {
        return getModuleConfig("events").getString("first-join.embed-color", "#FFAA00");
    }

    public String eventFirstJoinThumbnail() {
        return getModuleConfig("events").getString("first-join.embed-thumbnail", "https://mc-heads.net/avatar/%uuid%");
    }

    public boolean isEventLeaveEnabled() {
        return getModuleConfig("events").getBoolean("leave.enabled", true);
    }

    public String eventLeaveFormat() {
        return getModuleConfig("events").getString("leave.format", "**%displayname%** saiu do servidor.");
    }

    public String eventLeaveColor() {
        return getModuleConfig("events").getString("leave.embed-color", "#FF5555");
    }

    public String eventLeaveThumbnail() {
        return getModuleConfig("events").getString("leave.embed-thumbnail", "https://mc-heads.net/avatar/%uuid%");
    }

    public boolean isEventDeathEnabled() {
        return getModuleConfig("events").getBoolean("death.enabled", true);
    }

    public String eventDeathFormat() {
        return getModuleConfig("events").getString("death.format", "%deathmessage%");
    }

    public String eventDeathColor() {
        return getModuleConfig("events").getString("death.embed-color", "#000000");
    }

    public String eventDeathThumbnail() {
        return getModuleConfig("events").getString("death.embed-thumbnail", "https://mc-heads.net/avatar/%uuid%");
    }

    public boolean isEventAdvancementEnabled() {
        return getModuleConfig("events").getBoolean("advancement.enabled", true);
    }

    public String eventAdvancementFormat() {
        return getModuleConfig("events").getString("advancement.format", "**%displayname%** desbloqueou **%advancement%**");
    }

    public String eventAdvancementColor() {
        return getModuleConfig("events").getString("advancement.embed-color", "#FFAA00");
    }

    public String eventAdvancementThumbnail() {
        return getModuleConfig("events").getString("advancement.embed-thumbnail", "https://mc-heads.net/avatar/%uuid%");
    }

    // ── Synchronization ──────────────────────────────────────────

    public boolean isGroupRoleSyncEnabled() {
        return getModuleConfig("sync").getBoolean("group-role.enabled", false);
    }

    public boolean isGroupRoleSyncMinecraftAuthoritative() {
        return "minecraft".equalsIgnoreCase(getModuleConfig("sync").getString("group-role.authoritative", "minecraft"));
    }

    public boolean isGroupRoleSyncPrimaryOnly() {
        return getModuleConfig("sync").getBoolean("group-role.primary-group-only", true);
    }

    public int groupRoleSyncCycleMinutes() {
        return getModuleConfig("sync").getInt("group-role.cycle-interval-minutes", 0);
    }

    public boolean isNicknameSyncEnabled() {
        return getModuleConfig("sync").getBoolean("nickname.enabled", false);
    }

    public String nicknameSyncFormat() {
        return getModuleConfig("sync").getString("nickname.format", "%displayname%");
    }

    public int nicknameSyncCycleMinutes() {
        return getModuleConfig("sync").getInt("nickname.cycle-interval-minutes", 0);
    }

    // ── Console ──────────────────────────────────────────────────

    public boolean isConsoleEnabled() {
        return getModuleConfig("console").getBoolean("enabled", false);
    }

    public String consoleChannelId() {
        return getModuleConfig("console").getString("channel-id", "");
    }

    public String consoleCommandPrefix() {
        return getModuleConfig("console").getString("command-prefix", "!c");
    }

    public List<String> consoleBlockedCommands() {
        return getModuleConfig("console").getStringList("blocked-commands");
    }

    public List<String> consoleAllowedCommands() {
        return getModuleConfig("console").getStringList("allowed-commands");
    }

    public String consoleFormat() {
        return getModuleConfig("console").getString("format", "[{date} {level}] {message}");
    }

    public String consoleMinLevel() {
        return getModuleConfig("console").getString("min-level", "INFO");
    }

    // ── Canned Responses ─────────────────────────────────────────

    public boolean isCannedResponsesEnabled() {
        return getModuleConfig("responses").getBoolean("enabled", false);
    }

    // ── Status Webhook ───────────────────────────────────────────

    public boolean isStatusWebhookEnabled() {
        return getModuleConfig("status").getBoolean("enabled", false);
    }

    public String statusWebhookUrl() {
        return getModuleConfig("status").getString("url", "");
    }

    public String statusOnlineMessage() {
        return getModuleConfig("status").getString("online-message", "## 🟢 **Servidor está ligado.**");
    }

    public String statusOfflineMessage() {
        return getModuleConfig("status").getString("offline-message", "## 🛑 **Servidor foi desligado.**");
    }

    // ── Auto Repair ──────────────────────────────────────────────

    public boolean isAutoRepairEnabled() {
        return getModuleConfig("repair").getBoolean("enabled", true);
    }
}
