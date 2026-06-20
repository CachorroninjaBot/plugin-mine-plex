package com.haiz.servercore.metrics;

import java.util.UUID;

public record PlayerStats(
        UUID uuid,
        String name,
        long firstJoin,
        long lastJoin,
        long lastQuit,
        long totalPlaytimeSeconds,
        long totalSessions,
        long totalCommands,
        long totalChatMessages,
        long totalDeaths,
        long totalKills,
        long blocksBroken,
        long blocksPlaced,
        double distanceWalked
) {
    public long averageSessionSeconds() {
        return totalSessions <= 0 ? 0 : totalPlaytimeSeconds / totalSessions;
    }

    public String status(long nowSeconds) {
        long daysSinceFirstJoin = Math.max(0, (nowSeconds - firstJoin) / 86400);
        long daysSinceLastJoin = Math.max(0, (nowSeconds - Math.max(lastJoin, lastQuit)) / 86400);
        if (daysSinceFirstJoin <= 1) {
            return "novo";
        }
        if (daysSinceLastJoin >= 30) {
            return "inativo";
        }
        if (totalPlaytimeSeconds >= 7L * 86400L || totalSessions >= 100) {
            return "veterano";
        }
        if (totalSessions >= 10) {
            return "recorrente";
        }
        return "ativo";
    }
}
