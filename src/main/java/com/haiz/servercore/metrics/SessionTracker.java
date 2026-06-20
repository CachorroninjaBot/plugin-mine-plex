package com.haiz.servercore.metrics;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.utils.TimeUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionTracker {
    private final HaizServerCore plugin;
    private final Map<UUID, SessionData> sessions = new ConcurrentHashMap<>();

    public SessionTracker(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public void startSession(Player player) {
        long now = TimeUtils.nowSeconds();
        sessions.put(player.getUniqueId(), new SessionData(now, now, player.getLocation()));
    }

    public void touchMovement(Player player, Location from, Location to) {
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null) {
            return;
        }
        if (!from.getWorld().equals(to.getWorld())) {
            return;
        }
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        SessionData data = sessions.get(player.getUniqueId());
        if (data == null) {
            startSession(player);
            return;
        }
        data.lastMoveSeconds = TimeUtils.nowSeconds();
        data.lastLocation = to.clone();
        plugin.metrics().recordDistance(player.getUniqueId(), from.distance(to));
    }

    public void finishSession(Player player) {
        finishSession(player.getUniqueId());
    }

    public void finishSession(UUID uuid) {
        SessionData data = sessions.remove(uuid);
        if (data == null) {
            return;
        }
        long now = TimeUtils.nowSeconds();
        long duration = Math.max(0, now - data.joinSeconds);
        long afkSeconds = estimateAfkSeconds(data, now);
        plugin.metrics().saveSession(uuid, data.joinSeconds, now, duration, afkSeconds);
    }

    public void flushAll() {
        for (UUID uuid : sessions.keySet()) {
            finishSession(uuid);
        }
    }

    public long currentSessionSeconds(UUID uuid) {
        SessionData data = sessions.get(uuid);
        return data == null ? 0 : Math.max(0, TimeUtils.nowSeconds() - data.joinSeconds);
    }

    public int activePlayers() {
        long now = TimeUtils.nowSeconds();
        return (int) sessions.values().stream()
                .filter(data -> !isAfk(data, now))
                .count();
    }

    public int afkPlayers() {
        long now = TimeUtils.nowSeconds();
        return (int) sessions.values().stream()
                .filter(data -> isAfk(data, now))
                .count();
    }

    public long openSessionPlaytimeSeconds() {
        long now = TimeUtils.nowSeconds();
        return sessions.values().stream()
                .mapToLong(data -> Math.max(0, (now - data.dailyStartSeconds) - estimateAfkSeconds(data, now)))
                .sum();
    }

    public void resetDailyBaselines() {
        long now = TimeUtils.nowSeconds();
        sessions.values().forEach(data -> data.dailyStartSeconds = now);
    }

    private long estimateAfkSeconds(SessionData data, long now) {
        if (!plugin.config().isAfkDetectionEnabled()) {
            return 0;
        }
        long threshold = plugin.config().afkMinutesWithoutMove() * 60L;
        long idle = now - data.lastMoveSeconds;
        return idle > threshold ? idle - threshold : 0;
    }

    private boolean isAfk(SessionData data, long now) {
        if (!plugin.config().isAfkDetectionEnabled()) {
            return false;
        }
        return now - data.lastMoveSeconds > plugin.config().afkMinutesWithoutMove() * 60L;
    }

    private static final class SessionData {
        private final long joinSeconds;
        private volatile long dailyStartSeconds;
        private volatile long lastMoveSeconds;
        private volatile Location lastLocation;

        private SessionData(long joinSeconds, long lastMoveSeconds, Location lastLocation) {
            this.joinSeconds = joinSeconds;
            this.dailyStartSeconds = joinSeconds;
            this.lastMoveSeconds = lastMoveSeconds;
            this.lastLocation = lastLocation == null ? null : lastLocation.clone();
        }
    }
}
