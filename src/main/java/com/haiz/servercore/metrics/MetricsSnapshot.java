package com.haiz.servercore.metrics;

public record MetricsSnapshot(
        String dayKey,
        int onlinePlayers,
        int maxPlayers,
        double tps,
        double mspt,
        int uniquePlayersToday,
        int newPlayersToday,
        int peakOnlineToday,
        int activePlayers,
        int afkPlayers,
        int loadedWorlds,
        int loadedChunks,
        int entities,
        long usedMemoryMb,
        long maxMemoryMb,
        long uptimeSeconds,
        double averagePing,
        long totalSessionsToday,
        long totalPlaytimeToday,
        long commandsToday,
        long chatMessagesToday,
        long deathsToday,
        long killsToday,
        long blocksBrokenToday,
        long blocksPlacedToday,
        double distanceWalkedToday,
        long createdAt
) {
}
