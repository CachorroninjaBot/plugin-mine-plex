package com.haiz.servercore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private Set<String> sensitiveCommands = Set.of();
    private Set<String> dangerousConsoleCommands = Set.of();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        this.sensitiveCommands = lowerSet(config.getStringList("logs.commands.sensitive-commands"));
        this.dangerousConsoleCommands = lowerSet(config.getStringList("logs.commands.dangerous-console-commands"));
    }

    private Set<String> lowerSet(List<String> values) {
        Set<String> result = new HashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(value.toLowerCase(Locale.ROOT).replace("/", ""));
            }
        }
        return result;
    }

    public boolean isDiscordEnabled() {
        return plugin.getConfig().getBoolean("discord.enabled", true);
    }

    public String discordToken() {
        return plugin.getConfig().getString("discord.token", "");
    }

    public String guildId() {
        return plugin.getConfig().getString("discord.guild-id", "");
    }

    public String channelId(String channelKey) {
        return plugin.getConfig().getString("discord.channels." + channelKey, "");
    }

    public boolean isActivityEnabled() {
        return plugin.getConfig().getBoolean("discord.activity.enabled", true);
    }

    public String activityText() {
        return plugin.getConfig().getString("discord.activity.text", "Monitorando o servidor");
    }

    public long discordQueueDelayMillis() {
        return Math.max(250L, plugin.getConfig().getLong("discord.queue.send-delay-millis", 750L));
    }

    public int discordMaxBufferSize() {
        return Math.max(25, plugin.getConfig().getInt("discord.queue.max-buffer-size", 250));
    }

    public boolean isMetricsEnabled() {
        return plugin.getConfig().getBoolean("metrics.enabled", true);
    }

    public int snapshotIntervalSeconds() {
        return Math.max(15, plugin.getConfig().getInt("metrics.snapshot-interval-seconds", 60));
    }

    public int saveIntervalSeconds() {
        return Math.max(60, plugin.getConfig().getInt("metrics.save-interval-seconds", 300));
    }

    public boolean isAfkDetectionEnabled() {
        return plugin.getConfig().getBoolean("metrics.afk-detection.enabled", true);
    }

    public int afkMinutesWithoutMove() {
        return Math.max(1, plugin.getConfig().getInt("metrics.afk-detection.minutes-without-move", 10));
    }

    public boolean isDailyReportEnabled() {
        return plugin.getConfig().getBoolean("metrics.reports.daily.enabled", true);
    }

    public String dailyReportTime() {
        return plugin.getConfig().getString("metrics.reports.daily.time", "23:59");
    }

    public boolean isWeeklyReportEnabled() {
        return plugin.getConfig().getBoolean("metrics.reports.weekly.enabled", true);
    }

    public String weeklyReportDay() {
        return plugin.getConfig().getString("metrics.reports.weekly.day", "SUNDAY");
    }

    public String weeklyReportTime() {
        return plugin.getConfig().getString("metrics.reports.weekly.time", "23:59");
    }

    public boolean isJoinLeaveLogEnabled() {
        return plugin.getConfig().getBoolean("logs.join-leave.enabled", true);
    }

    public int returningAfterDays() {
        return Math.max(1, plugin.getConfig().getInt("logs.join-leave.returning-after-days", 14));
    }

    public boolean isCommandLogEnabled() {
        return plugin.getConfig().getBoolean("logs.commands.enabled", true);
    }

    public boolean shouldHideSensitiveArguments() {
        return plugin.getConfig().getBoolean("logs.commands.hide-sensitive-arguments", true);
    }

    public boolean isSensitiveCommand(String command) {
        return sensitiveCommands.contains(command.toLowerCase(Locale.ROOT).replace("/", ""));
    }

    public boolean isDangerousConsoleCommand(String command) {
        return dangerousConsoleCommands.contains(command.toLowerCase(Locale.ROOT).replace("/", ""));
    }

    public boolean isConsoleCommandLogEnabled() {
        return plugin.getConfig().getBoolean("logs.console.commands-enabled", true);
    }

    public boolean isConsoleOutputCaptureEnabled() {
        return plugin.getConfig().getBoolean("logs.console.output-capture-enabled", false);
    }

    public boolean isChatLogEnabled() {
        return plugin.getConfig().getBoolean("logs.chat.enabled", true);
    }

    public boolean isDeathLogEnabled() {
        return plugin.getConfig().getBoolean("logs.deaths.enabled", true);
    }

    public boolean isAdvancementLogEnabled() {
        return plugin.getConfig().getBoolean("logs.advancements.enabled", true);
    }

    public boolean isWorldChangeLogEnabled() {
        return plugin.getConfig().getBoolean("logs.world-change.enabled", true);
    }

    public boolean isKickLogEnabled() {
        return plugin.getConfig().getBoolean("logs.kick.enabled", true);
    }

    public boolean isBlockMetricsEnabled() {
        return plugin.getConfig().getBoolean("logs.blocks.enabled", true);
    }

    public boolean isMovementMetricsEnabled() {
        return plugin.getConfig().getBoolean("logs.movement.enabled", true);
    }

    public boolean areAlertsEnabled() {
        return plugin.getConfig().getBoolean("alerts.enabled", true);
    }

    public boolean isLowTpsAlertEnabled() {
        return areAlertsEnabled() && plugin.getConfig().getBoolean("alerts.low-tps.enabled", true);
    }

    public double lowTpsThreshold() {
        return plugin.getConfig().getDouble("alerts.low-tps.threshold", 17.0);
    }

    public int lowTpsCooldownSeconds() {
        return Math.max(30, plugin.getConfig().getInt("alerts.low-tps.cooldown-seconds", 300));
    }

    public boolean isMassJoinAlertEnabled() {
        return areAlertsEnabled() && plugin.getConfig().getBoolean("alerts.mass-join.enabled", true);
    }

    public int massJoinAmount() {
        return Math.max(2, plugin.getConfig().getInt("alerts.mass-join.amount", 8));
    }

    public int massJoinWindowSeconds() {
        return Math.max(5, plugin.getConfig().getInt("alerts.mass-join.within-seconds", 60));
    }

    public boolean isMassLeaveAlertEnabled() {
        return areAlertsEnabled() && plugin.getConfig().getBoolean("alerts.mass-leave.enabled", true);
    }

    public int massLeaveAmount() {
        return Math.max(2, plugin.getConfig().getInt("alerts.mass-leave.amount", 8));
    }

    public int massLeaveWindowSeconds() {
        return Math.max(5, plugin.getConfig().getInt("alerts.mass-leave.within-seconds", 60));
    }

    public boolean isCommandSpamAlertEnabled() {
        return areAlertsEnabled() && plugin.getConfig().getBoolean("alerts.command-spam.enabled", true);
    }

    public int commandSpamAmount() {
        return Math.max(2, plugin.getConfig().getInt("alerts.command-spam.amount", 10));
    }

    public int commandSpamWindowSeconds() {
        return Math.max(5, plugin.getConfig().getInt("alerts.command-spam.within-seconds", 30));
    }

    public boolean isNewRecordOnlineAlertEnabled() {
        return areAlertsEnabled() && plugin.getConfig().getBoolean("alerts.new-record-online.enabled", true);
    }
}
