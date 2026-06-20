package com.haiz.servercore.tasks;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.metrics.MetricsManager;
import org.bukkit.scheduler.BukkitRunnable;

public final class DatabaseSaveTask extends BukkitRunnable {
    private final HaizServerCore plugin;
    private final MetricsManager metricsManager;

    public DatabaseSaveTask(HaizServerCore plugin, MetricsManager metricsManager) {
        this.plugin = plugin;
        this.metricsManager = metricsManager;
    }

    @Override
    public void run() {
        if (plugin.database().isAvailable()) {
            metricsManager.saveDailyStats();
        }
    }
}
