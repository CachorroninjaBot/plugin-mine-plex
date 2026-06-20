package com.haiz.servercore.metrics;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.discord.DiscordEmbedFactory;
import com.haiz.servercore.discord.DiscordLogService;
import com.haiz.servercore.storage.DatabaseManager;
import com.haiz.servercore.tasks.DatabaseSaveTask;
import com.haiz.servercore.tasks.DiscordReportTask;
import com.haiz.servercore.tasks.RealtimeMetricsTask;
import com.haiz.servercore.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

public final class MetricsManager {
    private final HaizServerCore plugin;
    private final DatabaseManager database;
    private final DiscordLogService discordLogService;
    private final SessionTracker sessionTracker;
    private final EngagementTracker engagementTracker;
    private final ReportGenerator reportGenerator;
    private final Set<UUID> uniqueToday = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<UUID> newToday = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicInteger peakOnlineToday = new AtomicInteger();
    private final AtomicLong commandsToday = new AtomicLong();
    private final AtomicLong chatToday = new AtomicLong();
    private final AtomicLong sessionsToday = new AtomicLong();
    private final AtomicLong playtimeToday = new AtomicLong();
    private final AtomicLong deathsToday = new AtomicLong();
    private final AtomicLong killsToday = new AtomicLong();
    private final AtomicLong blocksBrokenToday = new AtomicLong();
    private final AtomicLong blocksPlacedToday = new AtomicLong();
    private final DoubleAdder distanceToday = new DoubleAdder();
    private final AtomicLong lowTpsLastAlert = new AtomicLong();
    private final Set<Integer> taskIds = new HashSet<>();
    private final long startedAtSeconds = TimeUtils.nowSeconds();
    private volatile String currentDayKey = TimeUtils.todayKey();

    public MetricsManager(HaizServerCore plugin, DatabaseManager database, DiscordLogService discordLogService) {
        this.plugin = plugin;
        this.database = database;
        this.discordLogService = discordLogService;
        this.sessionTracker = new SessionTracker(plugin);
        this.engagementTracker = new EngagementTracker(plugin);
        this.reportGenerator = new ReportGenerator(plugin, database);
    }

    public void start() {
        if (!plugin.config().isMetricsEnabled()) {
            return;
        }
        BukkitTask realtime = new RealtimeMetricsTask(plugin, this).runTaskTimer(
                plugin, 20L * plugin.config().snapshotIntervalSeconds(), 20L * plugin.config().snapshotIntervalSeconds());
        BukkitTask save = new DatabaseSaveTask(plugin, this).runTaskTimer(
                plugin, 20L * plugin.config().saveIntervalSeconds(), 20L * plugin.config().saveIntervalSeconds());
        BukkitTask reports = new DiscordReportTask(plugin, this).runTaskTimer(plugin, 20L * 60L, 20L * 60L);
        taskIds.add(realtime.getTaskId());
        taskIds.add(save.getTaskId());
        taskIds.add(reports.getTaskId());
    }

    public void stop() {
        sessionTracker.flushAll();
        for (Integer taskId : taskIds) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        taskIds.clear();
    }

    public void reload() {
        stop();
        start();
    }

    public void recordJoin(Player player, boolean firstJoin) {
        rotateDailyIfNeeded();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        uniqueToday.add(uuid);
        if (firstJoin) {
            newToday.add(uuid);
        }
        peakOnlineToday.updateAndGet(current -> Math.max(current, Bukkit.getOnlinePlayers().size()));
        sessionTracker.startSession(player);
        engagementTracker.recordJoin(player);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> database.players().upsertJoin(uuid, name, TimeUtils.nowSeconds()));
    }

    public void recordQuit(Player player) {
        engagementTracker.recordLeave(player);
        sessionTracker.finishSession(player);
    }

    public void saveSession(UUID uuid, long joinTime, long quitTime, long durationSeconds, long afkSeconds) {
        rotateDailyIfNeeded();
        sessionsToday.incrementAndGet();
        playtimeToday.addAndGet(Math.max(0, durationSeconds - afkSeconds));
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> database.players().finishSession(uuid, joinTime, quitTime, durationSeconds, afkSeconds));
    }

    public void recordCommand(Player player, String command) {
        rotateDailyIfNeeded();
        commandsToday.incrementAndGet();
        engagementTracker.recordCommand(player, command);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> database.players().incrementPlayerCounter(player.getUniqueId(), "total_commands", 1));
    }

    public void recordConsoleCommand() {
        rotateDailyIfNeeded();
        commandsToday.incrementAndGet();
    }

    public void recordChat(UUID uuid) {
        rotateDailyIfNeeded();
        chatToday.incrementAndGet();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> database.players().incrementPlayerCounter(uuid, "total_chat_messages", 1));
    }

    public void recordDeath(UUID uuid) {
        rotateDailyIfNeeded();
        deathsToday.incrementAndGet();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> database.players().incrementPlayerCounter(uuid, "total_deaths", 1));
    }

    public void recordKill(UUID uuid) {
        rotateDailyIfNeeded();
        killsToday.incrementAndGet();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> database.players().incrementPlayerCounter(uuid, "total_kills", 1));
    }

    public void recordBlockBroken(UUID uuid) {
        rotateDailyIfNeeded();
        blocksBrokenToday.incrementAndGet();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> database.players().incrementPlayerCounter(uuid, "blocks_broken", 1));
    }

    public void recordBlockPlaced(UUID uuid) {
        rotateDailyIfNeeded();
        blocksPlacedToday.incrementAndGet();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> database.players().incrementPlayerCounter(uuid, "blocks_placed", 1));
    }

    public void recordDistance(UUID uuid, double distance) {
        rotateDailyIfNeeded();
        distanceToday.add(Math.max(0, distance));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> database.players().addDistance(uuid, distance));
    }

    public void touchMovement(Player player, org.bukkit.Location from, org.bukkit.Location to) {
        sessionTracker.touchMovement(player, from, to);
    }

    public MetricsSnapshot snapshot() {
        rotateDailyIfNeeded();
        return collectSnapshot(currentDayKey);
    }

    private MetricsSnapshot collectSnapshot(String dayKey) {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        int online = onlinePlayers.size();
        peakOnlineToday.updateAndGet(current -> Math.max(current, online));
        double tps = currentTps();
        double mspt = tps <= 0 ? 0 : 1000.0 / Math.min(20.0, tps);
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1048576L;
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        int loadedChunks = 0;
        int entities = 0;
        for (World world : Bukkit.getWorlds()) {
            loadedChunks += world.getLoadedChunks().length;
            entities += world.getEntities().size();
        }
        double averagePing = onlinePlayers.stream()
                .mapToInt(Player::getPing)
                .average()
                .orElse(0);
        return new MetricsSnapshot(
                dayKey,
                online,
                Bukkit.getMaxPlayers(),
                tps,
                mspt,
                uniqueToday.size(),
                newToday.size(),
                peakOnlineToday.get(),
                sessionTracker.activePlayers(),
                sessionTracker.afkPlayers(),
                Bukkit.getWorlds().size(),
                loadedChunks,
                entities,
                usedMemory,
                maxMemory,
                Math.max(0, TimeUtils.nowSeconds() - startedAtSeconds),
                averagePing,
                sessionsToday.get(),
                playtimeToday.get() + sessionTracker.openSessionPlaytimeSeconds(),
                commandsToday.get(),
                chatToday.get(),
                deathsToday.get(),
                killsToday.get(),
                blocksBrokenToday.get(),
                blocksPlacedToday.get(),
                distanceToday.sum(),
                TimeUtils.nowSeconds()
        );
    }

    private synchronized void rotateDailyIfNeeded() {
        String today = TimeUtils.todayKey();
        if (today.equals(currentDayKey)) {
            return;
        }
        MetricsSnapshot closingSnapshot = collectSnapshot(currentDayKey);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> database.serverStats().saveDailyStats(closingSnapshot));
        currentDayKey = today;
        uniqueToday.clear();
        newToday.clear();
        peakOnlineToday.set(Bukkit.getOnlinePlayers().size());
        commandsToday.set(0);
        chatToday.set(0);
        sessionsToday.set(0);
        playtimeToday.set(0);
        deathsToday.set(0);
        killsToday.set(0);
        blocksBrokenToday.set(0);
        blocksPlacedToday.set(0);
        distanceToday.reset();
        sessionTracker.resetDailyBaselines();
    }

    public void saveSnapshot() {
        MetricsSnapshot snapshot = snapshot();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int previousPeak = database.serverStats().allTimePeakOnline();
            database.serverStats().saveSnapshot(snapshot);
            maybeAlertLowTps(snapshot);
            maybeAlertRecordOnline(snapshot, previousPeak);
        });
    }

    public void saveDailyStats() {
        MetricsSnapshot snapshot = snapshot();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> database.serverStats().saveDailyStats(snapshot));
    }

    public void sendDailyReport() {
        MetricsSnapshot snapshot = snapshot();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> discordLogService.metricsReport(reportGenerator.dailyReport(snapshot)));
        saveDailyStats();
    }

    public void sendWeeklyReport() {
        MetricsSnapshot snapshot = snapshot();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> discordLogService.metricsReport(reportGenerator.weeklyReport(snapshot)));
    }

    public ReportGenerator reports() {
        return reportGenerator;
    }

    private void maybeAlertLowTps(MetricsSnapshot snapshot) {
        if (!plugin.config().isLowTpsAlertEnabled() || snapshot.tps() <= 0 || snapshot.tps() >= plugin.config().lowTpsThreshold()) {
            return;
        }
        long now = TimeUtils.nowSeconds();
        long previous = lowTpsLastAlert.get();
        if (now - previous < plugin.config().lowTpsCooldownSeconds()) {
            return;
        }
        lowTpsLastAlert.set(now);
        discordLogService.alert(DiscordEmbedFactory.alert("TPS baixo",
                "TPS atual: " + String.format(java.util.Locale.US, "%.2f", snapshot.tps())
                        + " | Limite: " + plugin.config().lowTpsThreshold()));
    }

    private void maybeAlertRecordOnline(MetricsSnapshot snapshot, int allTimePeak) {
        if (!plugin.config().isNewRecordOnlineAlertEnabled() || !database.isAvailable()) {
            return;
        }
        if (snapshot.onlinePlayers() > allTimePeak) {
            discordLogService.alert(DiscordEmbedFactory.info("Novo recorde online",
                    "O servidor atingiu " + snapshot.onlinePlayers() + " jogadores online."));
        }
    }

    private double currentTps() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getTPS");
            double[] values = (double[]) method.invoke(Bukkit.getServer());
            return values.length == 0 ? -1 : values[0];
        } catch (Exception ignored) {
            return -1;
        }
    }
}
