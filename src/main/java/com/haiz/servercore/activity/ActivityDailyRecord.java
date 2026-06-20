package com.haiz.servercore.activity;

import java.util.UUID;

public record ActivityDailyRecord(
        String date,
        UUID uuid,
        String name,
        long playtimeSeconds,
        long joins,
        long commandsUsed,
        long deaths,
        long playerKills,
        long mobKills,
        long blocksBroken,
        long blocksPlaced,
        long advancements,
        double distanceWalked
) {
}
