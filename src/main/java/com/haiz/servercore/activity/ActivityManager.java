package com.haiz.servercore.activity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ActivityManager {
    private final JavaPlugin plugin;
    private final ActivityStorage storage;
    private volatile ActivityConfig config;
    private final Map<UUID, ActivityPlayerData> dailyCache = new ConcurrentHashMap<>();
    private final Map<UUID, ActivitySession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ActivityDailyRecord> pendingDaily = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ActivityClosedSession> pendingSessions = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ActivityCommandLog> pendingCommands = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> afterSaveCallbacks = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean saveRunning = new AtomicBoolean();

    public ActivityManager(JavaPlugin plugin, ActivityStorage storage, ActivityConfig config) {
        this.plugin = plugin;
        this.storage = storage;
        this.config = config;
    }

    public void reload(ActivityConfig config) {
        this.config = config;
    }

    public void restoreOnlinePlayers() {
        long now = now();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!ignored(player)) {
                openSession(player, now, false);
            }
        }
    }

    public void join(Player player) {
        if (ignored(player)) {
            return;
        }
        long now = now();
        boolean newPlayer = !player.hasPlayedBefore();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        openSession(player, now, true);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> storage.upsertJoin(uuid, name, now, newPlayer));
    }

    private void openSession(Player player, long timestamp, boolean countJoin) {
        ActivityPlayerData data = data(player);
        if (countJoin && config.trackJoinQuit()) {
            data.incrementJoins();
        }
        activeSessions.put(player.getUniqueId(), new ActivitySession(player, timestamp));
    }

    public void quit(Player player) {
        closeSession(player.getUniqueId(), player.getName(), now());
    }

    public void closeAllSessions() {
        long timestamp = now();
        for (ActivitySession session : new ArrayList<>(activeSessions.values())) {
            closeSession(session.uuid(), session.name(), timestamp);
        }
    }

    private void closeSession(UUID uuid, String name, long timestamp) {
        ActivitySession session = activeSessions.remove(uuid);
        if (session == null) {
            return;
        }
        if (config.trackPlaytime()) {
            data(uuid, name).addPlaytime(session.checkpoint(timestamp));
        }
        pendingSessions.add(new ActivityClosedSession(
                uuid, name, session.joinTime(), timestamp, Math.max(0, timestamp - session.joinTime())));
    }

    public void command(Player player, String message) {
        if (ignored(player) || !config.trackCommands() || (config.ignoreOpCommands() && player.isOp())) {
            return;
        }
        data(player).incrementCommands();
        String normalized = normalizeCommand(message);
        if (!normalized.isBlank()) {
            pendingCommands.add(new ActivityCommandLog(player.getUniqueId(), player.getName(), normalized, now()));
        }
    }

    public void death(Player player) {
        if (!ignored(player) && config.trackDeaths()) {
            data(player).incrementDeaths();
        }
    }

    public void playerKill(Player killer) {
        if (!ignored(killer) && config.trackKills()) {
            data(killer).incrementPlayerKills();
        }
    }

    public void mobKill(Player killer) {
        if (!ignored(killer) && config.trackMobsKilled()) {
            data(killer).incrementMobKills();
        }
    }

    public void blockBroken(Player player) {
        if (!ignored(player) && config.trackBlocksBroken()) {
            data(player).incrementBlocksBroken();
        }
    }

    public void blockPlaced(Player player) {
        if (!ignored(player) && config.trackBlocksPlaced()) {
            data(player).incrementBlocksPlaced();
        }
    }

    public void advancement(Player player) {
        if (!ignored(player) && config.trackAdvancements()) {
            data(player).incrementAdvancements();
        }
    }

    public void move(Player player, Location to) {
        if (ignored(player) || !config.trackDistanceWalked()) {
            return;
        }
        ActivitySession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            data(player).addDistance(session.recordMove(to));
        }
    }

    public void checkpointPlaytime() {
        if (!config.trackPlaytime()) {
            return;
        }
        long timestamp = now();
        for (ActivitySession session : activeSessions.values()) {
            data(session.uuid(), session.name()).addPlaytime(session.checkpoint(timestamp));
        }
    }

    public void saveAsync(Runnable after) {
        checkpointPlaytime();
        if (after != null) {
            afterSaveCallbacks.add(after);
        }
        if (!saveRunning.compareAndSet(false, true)) {
            return;
        }
        SaveBatch batch = drainBatch();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = storage.saveBatch(batch.daily(), batch.sessions(), batch.commands());
            if (!success) {
                restoreBatch(batch);
            }
            saveRunning.set(false);
            if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, this::runAfterSaveCallbacks);
            }
        });
    }

    private void runAfterSaveCallbacks() {
        Runnable callback;
        while ((callback = afterSaveCallbacks.poll()) != null) {
            callback.run();
        }
    }

    public void saveBlocking() {
        checkpointPlaytime();
        SaveBatch batch = drainBatch();
        if (!storage.saveBatch(batch.daily(), batch.sessions(), batch.commands())) {
            restoreBatch(batch);
        }
    }

    private SaveBatch drainBatch() {
        List<ActivityDailyRecord> records = new ArrayList<>();
        for (ActivityPlayerData data : dailyCache.values()) {
            ActivityDailyRecord record = data.flushDeltas();
            if (data.hasDeltas(record)) {
                records.add(record);
            }
        }
        drain(pendingDaily, records);
        List<ActivityClosedSession> sessions = new ArrayList<>();
        drain(pendingSessions, sessions);
        List<ActivityCommandLog> commands = new ArrayList<>();
        drain(pendingCommands, commands);
        return new SaveBatch(records, sessions, commands);
    }

    private void restoreBatch(SaveBatch batch) {
        pendingDaily.addAll(batch.daily());
        pendingSessions.addAll(batch.sessions());
        pendingCommands.addAll(batch.commands());
    }

    private <T> void drain(ConcurrentLinkedQueue<T> queue, List<T> target) {
        T value;
        while ((value = queue.poll()) != null) {
            target.add(value);
        }
    }

    private ActivityPlayerData data(Player player) {
        return data(player.getUniqueId(), player.getName());
    }

    private ActivityPlayerData data(UUID uuid, String name) {
        String today = LocalDate.now().toString();
        ActivityPlayerData data = dailyCache.computeIfAbsent(uuid, key -> new ActivityPlayerData(key, name, today));
        data.updateName(name);
        if (!today.equals(data.date())) {
            ActivityDailyRecord old = data.flushDeltas();
            if (data.hasDeltas(old)) {
                pendingDaily.add(old);
            }
            data.rotateDate(today);
        }
        return data;
    }

    public int activeSessionCount() {
        return activeSessions.size();
    }

    public boolean ignored(Player player) {
        return config.ignoreVanishedPlayers()
                && (player.hasMetadata("vanished") || player.hasMetadata("invisible"));
    }

    private String normalizeCommand(String message) {
        if (message == null) {
            return "";
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        if (!config.hideCommandArguments()) {
            return trimmed.substring(0, Math.min(trimmed.length(), 255));
        }
        int separator = trimmed.indexOf(' ');
        return separator < 0 ? trimmed : trimmed.substring(0, separator);
    }

    private long now() {
        return Instant.now().getEpochSecond();
    }

    private record SaveBatch(List<ActivityDailyRecord> daily, List<ActivityClosedSession> sessions,
                             List<ActivityCommandLog> commands) {
    }
}
