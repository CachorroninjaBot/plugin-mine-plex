package com.haiz.servercore.activity;

import com.haiz.servercore.HaizServerCore;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class HaizActivityModule {
    private final HaizServerCore plugin;
    private ActivityConfig config;
    private ActivityStorage storage;
    private ActivityManager manager;
    private ActivityStatsService stats;
    private ActivityDiscordService discord;
    private ActivityReportService reports;
    private ActivityListener listener;
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final AtomicInteger dailyPeak = new AtomicInteger();
    private final AtomicLong lastLowActivityAlert = new AtomicLong();
    private LocalDate peakDate = LocalDate.now();
    private volatile boolean running;
    private String lastDailyReport = "";
    private String lastWeeklyReport = "";

    public HaizActivityModule(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public synchronized void start() {
        registerCommand();
        this.config = new ActivityConfig(plugin.getConfig());
        if (!config.enabled()) {
            plugin.getLogger().info("[HaizActivity] Sistema desativado por configuracao.");
            return;
        }

        this.storage = new ActivityStorage(plugin, config);
        storage.open();
        if (!storage.isAvailable()) {
            plugin.getLogger().warning("[HaizActivity] Modulo nao iniciado porque o SQLite esta indisponivel.");
            return;
        }

        this.manager = new ActivityManager(plugin, storage, config);
        this.stats = new ActivityStatsService(storage);
        this.discord = new ActivityDiscordService(plugin, config);
        this.reports = new ActivityReportService(plugin, stats, new ActivityInsightService(), discord, config);
        this.listener = new ActivityListener(manager, this);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        manager.restoreOnlinePlayers();
        scheduleTasks();
        running = true;

        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> dailyPeak.set(storage.peakForDate(LocalDate.now().toString())));
        if (!discord.isConfigured()) {
            plugin.getLogger().info("[HaizActivity] Webhook nao configurado. Relatorios Discord desativados.");
        }
        plugin.getLogger().info("[HaizActivity] Sistema iniciado.");
    }

    public synchronized void stop() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (manager != null) {
            manager.closeAllSessions();
            manager.saveBlocking();
        }
        if (storage != null) {
            storage.close();
        }
        running = false;
        manager = null;
        stats = null;
        reports = null;
        discord = null;
        storage = null;
    }

    public synchronized void reload() {
        stop();
        start();
    }

    private void registerCommand() {
        PluginCommand command = plugin.getCommand("haizactivity");
        if (command != null) {
            ActivityCommand executor = new ActivityCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    private void scheduleTasks() {
        long saveTicks = config.autosaveMinutes() * 60L * 20L;
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> manager.saveAsync(null), saveTicks, saveTicks));

        if (config.trackOnlineSnapshots()) {
            long snapshotTicks = config.snapshotMinutes() * 60L * 20L;
            tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::captureSnapshot, 20L, snapshotTicks));
        }

        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::checkSchedules, 20L, 20L * 30L));
        if (config.lowActivityAlertEnabled()) {
            tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::checkLowActivity,
                    20L * 60L, 20L * 60L * 5L));
        }
    }

    private void captureSnapshot() {
        int online = (int) Bukkit.getOnlinePlayers().stream().filter(player -> !manager.ignored(player)).count();
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L / 1024L;
        double tps = currentTps();
        long timestamp = Instant.now().getEpochSecond();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> storage.saveSnapshot(timestamp, online, tps, usedMemory));
        checkPeak(online);
    }

    private double currentTps() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getTPS");
            Object value = method.invoke(Bukkit.getServer());
            if (value instanceof double[] tps && tps.length > 0) {
                return tps[0];
            }
        } catch (ReflectiveOperationException ignored) {
            // Spigot does not expose TPS through the public API.
        }
        return -1;
    }

    private void checkSchedules() {
        LocalDateTime now = LocalDateTime.now();
        if (config.dailyReportEnabled() && matchesTime(now.toLocalTime(), config.dailyReportTime())) {
            String key = now.toLocalDate().toString();
            if (!key.equals(lastDailyReport)) {
                lastDailyReport = key;
                sendDailyReport();
            }
        }
        if (config.weeklyReportEnabled()
                && now.getDayOfWeek() == config.weeklyReportDay()
                && matchesTime(now.toLocalTime(), config.weeklyReportTime())) {
            WeekFields fields = WeekFields.of(Locale.getDefault());
            String key = now.getYear() + "-" + now.get(fields.weekOfWeekBasedYear());
            if (!key.equals(lastWeeklyReport)) {
                lastWeeklyReport = key;
                sendWeeklyReport();
            }
        }
    }

    private boolean matchesTime(LocalTime now, String configured) {
        try {
            LocalTime target = LocalTime.parse(configured);
            return now.getHour() == target.getHour() && now.getMinute() == target.getMinute();
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private void checkLowActivity() {
        if (!running || !discord.isConfigured() || !Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long latest = storage.latestActivityTimestamp();
            long now = Instant.now().getEpochSecond();
            long threshold = Duration.ofHours(config.inactiveHoursThreshold()).toSeconds();
            if (latest <= 0 || now - latest < threshold || now - lastLowActivityAlert.get() < threshold) {
                return;
            }
            String player = storage.latestPlayerName();
            ActivityEmbed embed = new ActivityEmbed(
                    "Baixa atividade detectada",
                    "O servidor esta sem jogadores ha pelo menos " + config.inactiveHoursThreshold() + " horas.",
                    0xE67E22
            ).addField("Ultimo jogador", player, true)
                    .addField("Ultima atividade", "<t:" + latest + ":R>", true);
            discord.send(embed).thenAccept(success -> {
                if (success) {
                    lastLowActivityAlert.set(now);
                }
            });
        });
    }

    public void handleJoinAlerts(Player player) {
        checkPeak(Bukkit.getOnlinePlayers().size());
        if (config.newPlayerAlertEnabled() && !player.hasPlayedBefore() && discord.isConfigured()) {
            discord.send(new ActivityEmbed(
                    "Novo jogador entrou no servidor",
                    "**Jogador:** " + player.getName() + "\nPrimeira entrada registrada agora.",
                    0x2ECC71));
        }
    }

    private void checkPeak(int online) {
        if (!running || !config.dailyPeakAlertEnabled() || !discord.isConfigured() || online <= 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (!today.equals(peakDate)) {
            peakDate = today;
            dailyPeak.set(0);
        }
        int previous = dailyPeak.getAndAccumulate(online, Math::max);
        if (online > previous) {
            discord.send(new ActivityEmbed(
                    "Novo pico diario!",
                    "O servidor atingiu **" + online + " jogadores online** hoje.",
                    0xF1C40F));
        }
    }

    public void sendDailyReport() {
        if (!running) {
            return;
        }
        manager.saveAsync(() -> Bukkit.getScheduler().runTaskAsynchronously(plugin, reports::sendDaily));
    }

    public void sendWeeklyReport() {
        if (!running) {
            return;
        }
        manager.saveAsync(() -> Bukkit.getScheduler().runTaskAsynchronously(plugin, reports::sendWeekly));
    }

    public void sendTestDiscord() {
        if (running) {
            discord.sendTest();
        }
    }

    public void queryAsync(Runnable query) {
        if (!running) {
            return;
        }
        manager.saveAsync(() -> Bukkit.getScheduler().runTaskAsynchronously(plugin, query));
    }

    public void respond(org.bukkit.command.CommandSender sender, List<String> lines) {
        Bukkit.getScheduler().runTask(plugin, () -> lines.forEach(sender::sendMessage));
    }

    public boolean isRunning() {
        return running;
    }

    public ActivityConfig config() {
        return config;
    }

    public ActivityStatsService stats() {
        return stats;
    }

    public ActivityManager manager() {
        return manager;
    }

    public ActivityDiscordService discord() {
        return discord;
    }

    public String storageState() {
        return storage != null && storage.isAvailable() ? "SQLite conectado" : "indisponivel";
    }

    public int onlineTracked() {
        return manager == null ? 0 : manager.activeSessionCount();
    }

    public String nextDailyTime() {
        return config == null ? "-" : config.dailyReportTime();
    }

    public HaizServerCore plugin() {
        return plugin;
    }
}
