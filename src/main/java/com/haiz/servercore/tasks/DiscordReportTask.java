package com.haiz.servercore.tasks;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.metrics.MetricsManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;

public final class DiscordReportTask extends BukkitRunnable {
    private final HaizServerCore plugin;
    private final MetricsManager metricsManager;
    private String lastDailyKey = "";
    private String lastWeeklyKey = "";

    public DiscordReportTask(HaizServerCore plugin, MetricsManager metricsManager) {
        this.plugin = plugin;
        this.metricsManager = metricsManager;
    }

    @Override
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        if (plugin.config().isDailyReportEnabled() && isDue(now.toLocalTime(), plugin.config().dailyReportTime())) {
            String key = now.toLocalDate().toString();
            if (!key.equals(lastDailyKey)) {
                lastDailyKey = key;
                metricsManager.sendDailyReport();
            }
        }
        if (plugin.config().isWeeklyReportEnabled() && weeklyDayMatches(now) && isDue(now.toLocalTime(), plugin.config().weeklyReportTime())) {
            String key = now.toLocalDate().toString();
            if (!key.equals(lastWeeklyKey)) {
                lastWeeklyKey = key;
                metricsManager.sendWeeklyReport();
            }
        }
    }

    private boolean isDue(LocalTime now, String configured) {
        try {
            LocalTime target = LocalTime.parse(configured);
            return Math.abs(now.toSecondOfDay() - target.toSecondOfDay()) < 60;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean weeklyDayMatches(LocalDateTime now) {
        try {
            return now.getDayOfWeek() == DayOfWeek.valueOf(plugin.config().weeklyReportDay().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return now.getDayOfWeek() == DayOfWeek.SUNDAY;
        }
    }
}
