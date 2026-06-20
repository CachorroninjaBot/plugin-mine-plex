package com.haiz.servercore.activity;

import java.util.UUID;

public record ActivityPlayerProfile(
        UUID uuid,
        String name,
        long firstJoin,
        long lastJoin,
        long lastSeen,
        long totalPlaytimeSeconds,
        long sessions,
        long todayPlaytimeSeconds,
        long todayCommands,
        long todayDeaths,
        long todayBlocksBroken
) {
}
