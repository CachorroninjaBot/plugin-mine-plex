package com.haiz.servercore.activity;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.DayOfWeek;
import java.util.Locale;

public final class ActivityConfig {
    private final FileConfiguration config;

    public ActivityConfig(FileConfiguration config) {
        this.config = config;
    }

    public boolean enabled() {
        return config.getBoolean("activity.enabled", true);
    }

    public String storageFile() {
        return config.getString("activity.storage.file", "activity.db");
    }

    public int autosaveMinutes() {
        return Math.max(1, config.getInt("activity.storage.autosave-minutes", 5));
    }

    public boolean trackJoinQuit() {
        return tracking("join-quit", true);
    }

    public boolean trackPlaytime() {
        return tracking("playtime", true);
    }

    public boolean trackCommands() {
        return tracking("commands", true);
    }

    public boolean trackDeaths() {
        return tracking("deaths", true);
    }

    public boolean trackKills() {
        return tracking("kills", true);
    }

    public boolean trackBlocksBroken() {
        return tracking("blocks-broken", true);
    }

    public boolean trackBlocksPlaced() {
        return tracking("blocks-placed", true);
    }

    public boolean trackMobsKilled() {
        return tracking("mobs-killed", true);
    }

    public boolean trackAdvancements() {
        return tracking("advancements", true);
    }

    public boolean trackDistanceWalked() {
        return tracking("distance-walked", true);
    }

    public boolean trackOnlineSnapshots() {
        return tracking("online-snapshots", true);
    }

    public int snapshotMinutes() {
        return Math.max(1, config.getInt("activity.tracking.snapshot-minutes", 5));
    }

    private boolean tracking(String key, boolean fallback) {
        return config.getBoolean("activity.tracking." + key, fallback);
    }

    public boolean discordEnabled() {
        return config.getBoolean("activity.discord.enabled", true);
    }

    public String webhookUrl() {
        return config.getString("activity.discord.webhook-url", "");
    }

    public boolean dailyReportEnabled() {
        return config.getBoolean("activity.discord.daily-report.enabled", true);
    }

    public String dailyReportTime() {
        return config.getString("activity.discord.daily-report.time", "23:59");
    }

    public boolean weeklyReportEnabled() {
        return config.getBoolean("activity.discord.weekly-report.enabled", true);
    }

    public DayOfWeek weeklyReportDay() {
        String value = config.getString("activity.discord.weekly-report.day", "SUNDAY");
        try {
            return DayOfWeek.valueOf(value == null ? "SUNDAY" : value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return DayOfWeek.SUNDAY;
        }
    }

    public String weeklyReportTime() {
        return config.getString("activity.discord.weekly-report.time", "23:59");
    }

    public boolean alertsEnabled() {
        return config.getBoolean("activity.discord.alerts.enabled", true);
    }

    public boolean lowActivityAlertEnabled() {
        return alertsEnabled() && config.getBoolean("activity.discord.alerts.low-activity", true);
    }

    public int inactiveHoursThreshold() {
        return Math.max(1, config.getInt("activity.discord.alerts.inactive-hours-threshold", 3));
    }

    public boolean dailyPeakAlertEnabled() {
        return alertsEnabled() && config.getBoolean("activity.discord.alerts.daily-peak", true);
    }

    public boolean newPlayerAlertEnabled() {
        return alertsEnabled() && config.getBoolean("activity.discord.alerts.new-player", false);
    }

    public int dailyRankingLimit() {
        return Math.max(1, config.getInt("activity.rankings.daily-limit", 10));
    }

    public int weeklyRankingLimit() {
        return Math.max(1, config.getInt("activity.rankings.weekly-limit", 10));
    }

    public boolean ignoreVanishedPlayers() {
        return config.getBoolean("activity.privacy.ignore-vanished-players", true);
    }

    public boolean ignoreOpCommands() {
        return config.getBoolean("activity.privacy.ignore-op-commands", false);
    }

    public boolean hideCommandArguments() {
        return config.getBoolean("activity.privacy.hide-command-arguments", true);
    }
}
