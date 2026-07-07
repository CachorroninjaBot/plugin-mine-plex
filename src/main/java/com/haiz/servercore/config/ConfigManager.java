package com.haiz.servercore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ConfigManager {
    private final JavaPlugin plugin;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    // ── Discord Bot ──────────────────────────────────────────────

    public boolean isDiscordEnabled() {
        return plugin.getConfig().getBoolean("discord.enabled", true);
    }

    public String discordToken() {
        return plugin.getConfig().getString("discord.token", "");
    }

    public String guildId() {
        return plugin.getConfig().getString("discord.guild-id", "");
    }

    public boolean isActivityEnabled() {
        return plugin.getConfig().getBoolean("discord.activity.enabled", true);
    }

    public String activityText() {
        return plugin.getConfig().getString("discord.activity.text", "Monitorando o servidor");
    }

    public String activityType() {
        return plugin.getConfig().getString("discord.activity.type", "PLAYING");
    }

    // ── Chat Bridge ──────────────────────────────────────────────

    public boolean isChatBridgeEnabled() {
        return plugin.getConfig().getBoolean("chat-bridge.enabled", false);
    }

    public boolean isDiscordToMinecraftEnabled() {
        return plugin.getConfig().getBoolean("chat-bridge.discord-to-minecraft.enabled", false);
    }

    public List<String> chatBridgeChannelIds() {
        return plugin.getConfig().getStringList("chat-bridge.discord-to-minecraft.channel-ids");
    }

    public String discordToMinecraftFormat() {
        return plugin.getConfig().getString("chat-bridge.discord-to-minecraft.format", "&b[Discord] &e%name% &7>> &f%message%");
    }

    public boolean chatBridgeBlockBots() {
        return plugin.getConfig().getBoolean("chat-bridge.discord-to-minecraft.block-bots", true);
    }

    public String chatBridgeEmojiBehavior() {
        return plugin.getConfig().getString("chat-bridge.discord-to-minecraft.emoji-behavior", "show");
    }

    public String chatBridgeMentionBehavior() {
        return plugin.getConfig().getString("chat-bridge.discord-to-minecraft.mention-behavior", "show");
    }

    public int chatBridgeMaxLength() {
        return plugin.getConfig().getInt("chat-bridge.discord-to-minecraft.max-length", 256);
    }

    public boolean isMinecraftToDiscordEnabled() {
        return plugin.getConfig().getBoolean("chat-bridge.minecraft-to-discord.enabled", false);
    }

    public String minecraftToDiscordChannelId() {
        return plugin.getConfig().getString("chat-bridge.minecraft-to-discord.channel-id", "");
    }

    public String minecraftToDiscordFormat() {
        return plugin.getConfig().getString("chat-bridge.minecraft-to-discord.format", "**%primarygroup%** %displayname% >> %message%");
    }

    public String minecraftToDiscordPrefix() {
        return plugin.getConfig().getString("chat-bridge.minecraft-to-discord.prefix-required", "");
    }

    public boolean isChatBridgeWebhookEnabled() {
        return plugin.getConfig().getBoolean("chat-bridge.minecraft-to-discord.use-webhook", false);
    }

    public String chatBridgeWebhookAvatar() {
        return plugin.getConfig().getString("chat-bridge.minecraft-to-discord.webhook-avatar", "https://mc-heads.net/avatar/%uuid%");
    }

    public String chatBridgeWebhookName() {
        return plugin.getConfig().getString("chat-bridge.minecraft-to-discord.webhook-name", "%displayname%");
    }

    public boolean isReserializerToDiscord() {
        return plugin.getConfig().getBoolean("chat-bridge.reserializer.to-discord", true);
    }

    public boolean isReserializerToMinecraft() {
        return plugin.getConfig().getBoolean("chat-bridge.reserializer.to-minecraft", true);
    }

    // ── Account Linking ──────────────────────────────────────────

    public boolean isAccountLinkingEnabled() {
        return plugin.getConfig().getBoolean("account-linking.enabled", true);
    }

    public int accountLinkingCodeLength() {
        return plugin.getConfig().getInt("account-linking.code-length", 4);
    }

    public int accountLinkingCodeExpiry() {
        return plugin.getConfig().getInt("account-linking.code-expiry-seconds", 120);
    }

    public String accountLinkingRoleId() {
        return plugin.getConfig().getString("account-linking.linked-role-id", "");
    }

    public List<String> accountLinkingOnLinkCommands() {
        return plugin.getConfig().getStringList("account-linking.on-link-commands");
    }

    public List<String> accountLinkingOnUnlinkCommands() {
        return plugin.getConfig().getStringList("account-linking.on-unlink-commands");
    }

    // ── Events ───────────────────────────────────────────────────

    public boolean isEventsEnabled() {
        return plugin.getConfig().getBoolean("events.enabled", false);
    }

    public String eventsChannelId() {
        return plugin.getConfig().getString("events.channel-id", "");
    }

    public boolean isEventsWebhookEnabled() {
        return plugin.getConfig().getBoolean("events.use-webhook", false);
    }

    public String eventsWebhookAvatar() {
        return plugin.getConfig().getString("events.webhook-avatar", "https://mc-heads.net/avatar/%uuid%");
    }

    public boolean isEventJoinEnabled() {
        return plugin.getConfig().getBoolean("events.join.enabled", true);
    }

    public String eventJoinFormat() {
        return plugin.getConfig().getString("events.join.format", "**%displayname%** entrou no servidor.");
    }

    public String eventJoinColor() {
        return plugin.getConfig().getString("events.join.embed-color", "#55FF55");
    }

    public String eventJoinThumbnail() {
        return plugin.getConfig().getString("events.join.embed-thumbnail", "https://mc-heads.net/avatar/%uuid%");
    }

    public boolean isFirstJoinEnabled() {
        return plugin.getConfig().getBoolean("events.first-join.enabled", true);
    }

    public String eventFirstJoinFormat() {
        return plugin.getConfig().getString("events.first-join.format", "**%displayname%** entrou pela primeira vez!");
    }

    public String eventFirstJoinColor() {
        return plugin.getConfig().getString("events.first-join.embed-color", "#FFAA00");
    }

    public String eventFirstJoinThumbnail() {
        return plugin.getConfig().getString("events.first-join.embed-thumbnail", "https://mc-heads.net/avatar/%uuid%");
    }

    public boolean isEventLeaveEnabled() {
        return plugin.getConfig().getBoolean("events.leave.enabled", true);
    }

    public String eventLeaveFormat() {
        return plugin.getConfig().getString("events.leave.format", "**%displayname%** saiu do servidor.");
    }

    public String eventLeaveColor() {
        return plugin.getConfig().getString("events.leave.embed-color", "#FF5555");
    }

    public String eventLeaveThumbnail() {
        return plugin.getConfig().getString("events.leave.embed-thumbnail", "https://mc-heads.net/avatar/%uuid%");
    }

    public boolean isEventDeathEnabled() {
        return plugin.getConfig().getBoolean("events.death.enabled", true);
    }

    public String eventDeathFormat() {
        return plugin.getConfig().getString("events.death.format", "%deathmessage%");
    }

    public String eventDeathColor() {
        return plugin.getConfig().getString("events.death.embed-color", "#000000");
    }

    public String eventDeathThumbnail() {
        return plugin.getConfig().getString("events.death.embed-thumbnail", "https://mc-heads.net/avatar/%uuid%");
    }

    public boolean isEventAdvancementEnabled() {
        return plugin.getConfig().getBoolean("events.advancement.enabled", true);
    }

    public String eventAdvancementFormat() {
        return plugin.getConfig().getString("events.advancement.format", "**%displayname%** desbloqueou **%advancement%**");
    }

    public String eventAdvancementColor() {
        return plugin.getConfig().getString("events.advancement.embed-color", "#FFAA00");
    }

    public String eventAdvancementThumbnail() {
        return plugin.getConfig().getString("events.advancement.embed-thumbnail", "https://mc-heads.net/avatar/%uuid%");
    }

    // ── Synchronization ──────────────────────────────────────────

    public boolean isGroupRoleSyncEnabled() {
        return plugin.getConfig().getBoolean("synchronization.group-role.enabled", false);
    }

    public boolean isGroupRoleSyncMinecraftAuthoritative() {
        return "minecraft".equalsIgnoreCase(plugin.getConfig().getString("synchronization.group-role.authoritative", "minecraft"));
    }

    public boolean isGroupRoleSyncPrimaryOnly() {
        return plugin.getConfig().getBoolean("synchronization.group-role.primary-group-only", true);
    }

    public int groupRoleSyncCycleMinutes() {
        return plugin.getConfig().getInt("synchronization.group-role.cycle-interval-minutes", 0);
    }

    public boolean isNicknameSyncEnabled() {
        return plugin.getConfig().getBoolean("synchronization.nickname.enabled", false);
    }

    public String nicknameSyncFormat() {
        return plugin.getConfig().getString("synchronization.nickname.format", "%displayname%");
    }

    public int nicknameSyncCycleMinutes() {
        return plugin.getConfig().getInt("synchronization.nickname.cycle-interval-minutes", 0);
    }

    // ── Console ──────────────────────────────────────────────────

    public boolean isConsoleEnabled() {
        return plugin.getConfig().getBoolean("console.enabled", false);
    }

    public String consoleChannelId() {
        return plugin.getConfig().getString("console.channel-id", "");
    }

    public String consoleCommandPrefix() {
        return plugin.getConfig().getString("console.command-prefix", "!c");
    }

    public List<String> consoleBlockedCommands() {
        return plugin.getConfig().getStringList("console.blocked-commands");
    }

    public List<String> consoleAllowedCommands() {
        return plugin.getConfig().getStringList("console.allowed-commands");
    }

    public String consoleFormat() {
        return plugin.getConfig().getString("console.format", "[{date} {level}] {message}");
    }

    public String consoleMinLevel() {
        return plugin.getConfig().getString("console.min-level", "INFO");
    }

    // ── Canned Responses ─────────────────────────────────────────

    public boolean isCannedResponsesEnabled() {
        return plugin.getConfig().getBoolean("canned-responses.enabled", false);
    }

    // ── Status Webhook ───────────────────────────────────────────

    public boolean isStatusWebhookEnabled() {
        return plugin.getConfig().getBoolean("status-webhook.enabled", false);
    }

    public String statusWebhookUrl() {
        return plugin.getConfig().getString("status-webhook.url", "");
    }

    public String statusOnlineMessage() {
        return plugin.getConfig().getString("status-webhook.online-message", "## 🟢 **Servidor está ligado.**");
    }

    public String statusOfflineMessage() {
        return plugin.getConfig().getString("status-webhook.offline-message", "## 🛑 **Servidor foi desligado.**");
    }

    // ── VIP (delegado ao VipConfig) ─────────────────────────────

    // VIP config é lido diretamente pelo VipConfig

    // ── Auto Repair ──────────────────────────────────────────────

    public boolean isAutoRepairEnabled() {
        return plugin.getConfig().getBoolean("auto-repair.enabled", true);
    }
}
