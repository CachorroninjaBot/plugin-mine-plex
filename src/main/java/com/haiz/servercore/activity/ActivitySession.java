package com.haiz.servercore.activity;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class ActivitySession {
    private final UUID uuid;
    private String name;
    private final long joinTime;
    private long lastCheckpointTime;
    private String lastWorld;
    private double lastX;
    private double lastY;
    private double lastZ;

    public ActivitySession(Player player, long joinTime) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.joinTime = joinTime;
        this.lastCheckpointTime = joinTime;
        updateLastLocation(player.getLocation());
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public long joinTime() {
        return joinTime;
    }

    public synchronized long checkpoint(long timestamp) {
        long elapsed = Math.max(0, timestamp - lastCheckpointTime);
        lastCheckpointTime = Math.max(lastCheckpointTime, timestamp);
        return elapsed;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public double recordMove(Location to) {
        if (to == null || to.getWorld() == null) {
            return 0;
        }
        String worldName = to.getWorld().getName();
        if (lastWorld == null || !lastWorld.equals(worldName)) {
            updateLastLocation(to);
            return 0;
        }
        double distance = Math.sqrt(square(to.getX() - lastX) + square(to.getY() - lastY) + square(to.getZ() - lastZ));
        updateLastLocation(to);
        if (distance > 50) {
            return 0;
        }
        return distance;
    }

    private void updateLastLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        this.lastWorld = location.getWorld().getName();
        this.lastX = location.getX();
        this.lastY = location.getY();
        this.lastZ = location.getZ();
    }

    private double square(double value) {
        return value * value;
    }
}
