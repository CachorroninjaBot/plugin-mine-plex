package com.haiz.servercore.metrics;

public record MetricsPeriodSummary(
        int requestedDays,
        int storedDays,
        long uniquePlayers,
        long newPlayers,
        int peakOnline,
        long totalPlaytimeSeconds,
        long totalSessions,
        long averageSessionSeconds,
        long commandsExecuted,
        long chatMessages,
        long deaths,
        long kills,
        long blocksBroken,
        long blocksPlaced,
        double distanceWalked,
        double averageTps,
        double minimumTps,
        double maximumMspt
) {
    public static MetricsPeriodSummary empty(int requestedDays) {
        return new MetricsPeriodSummary(requestedDays, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0);
    }
}
