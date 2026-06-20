package com.haiz.servercore.activity;

public record ActivityPeriodSummary(
        String fromDate,
        String toDate,
        long uniquePlayers,
        long newPlayers,
        long totalPlaytimeSeconds,
        long joins,
        long commandsUsed,
        long deaths,
        long playerKills,
        long mobKills,
        long blocksBroken,
        long blocksPlaced,
        long advancements,
        double distanceWalked,
        int peakOnline,
        double averageOnline,
        int busiestHour
) {
    public long averagePlaytimeSeconds() {
        return uniquePlayers <= 0 ? 0 : totalPlaytimeSeconds / uniquePlayers;
    }
}
