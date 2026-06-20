package com.haiz.servercore.activity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

public final class ActivityPlayerData {
    private final UUID uuid;
    private volatile String name;
    private volatile String date;
    private final AtomicLong playtimeSeconds = new AtomicLong();
    private final AtomicLong joins = new AtomicLong();
    private final AtomicLong commandsUsed = new AtomicLong();
    private final AtomicLong deaths = new AtomicLong();
    private final AtomicLong playerKills = new AtomicLong();
    private final AtomicLong mobKills = new AtomicLong();
    private final AtomicLong blocksBroken = new AtomicLong();
    private final AtomicLong blocksPlaced = new AtomicLong();
    private final AtomicLong advancements = new AtomicLong();
    private final DoubleAdder distanceWalked = new DoubleAdder();

    public ActivityPlayerData(UUID uuid, String name, String date) {
        this.uuid = uuid;
        this.name = name;
        this.date = date;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public String date() {
        return date;
    }

    public void updateName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public synchronized void rotateDate(String date) {
        if (this.date.equals(date)) {
            return;
        }
        this.date = date;
        playtimeSeconds.set(0);
        joins.set(0);
        commandsUsed.set(0);
        deaths.set(0);
        playerKills.set(0);
        mobKills.set(0);
        blocksBroken.set(0);
        blocksPlaced.set(0);
        advancements.set(0);
        distanceWalked.reset();
    }

    public void incrementJoins() {
        joins.incrementAndGet();
    }

    public void addPlaytime(long seconds) {
        playtimeSeconds.addAndGet(Math.max(0, seconds));
    }

    public void incrementCommands() {
        commandsUsed.incrementAndGet();
    }

    public void incrementDeaths() {
        deaths.incrementAndGet();
    }

    public void incrementPlayerKills() {
        playerKills.incrementAndGet();
    }

    public void incrementMobKills() {
        mobKills.incrementAndGet();
    }

    public void incrementBlocksBroken() {
        blocksBroken.incrementAndGet();
    }

    public void incrementBlocksPlaced() {
        blocksPlaced.incrementAndGet();
    }

    public void incrementAdvancements() {
        advancements.incrementAndGet();
    }

    public void addDistance(double distance) {
        if (distance > 0) {
            distanceWalked.add(distance);
        }
    }

    public ActivityDailyRecord flushDeltas() {
        return new ActivityDailyRecord(
                date,
                uuid,
                name,
                playtimeSeconds.getAndSet(0),
                joins.getAndSet(0),
                commandsUsed.getAndSet(0),
                deaths.getAndSet(0),
                playerKills.getAndSet(0),
                mobKills.getAndSet(0),
                blocksBroken.getAndSet(0),
                blocksPlaced.getAndSet(0),
                advancements.getAndSet(0),
                distanceWalked.sumThenReset()
        );
    }

    public ActivityDailyRecord peek() {
        return new ActivityDailyRecord(
                date,
                uuid,
                name,
                playtimeSeconds.get(),
                joins.get(),
                commandsUsed.get(),
                deaths.get(),
                playerKills.get(),
                mobKills.get(),
                blocksBroken.get(),
                blocksPlaced.get(),
                advancements.get(),
                distanceWalked.sum()
        );
    }

    public void merge(ActivityDailyRecord record) {
        if (!uuid.equals(record.uuid()) || !date.equals(record.date())) {
            return;
        }
        playtimeSeconds.addAndGet(record.playtimeSeconds());
        joins.addAndGet(record.joins());
        commandsUsed.addAndGet(record.commandsUsed());
        deaths.addAndGet(record.deaths());
        playerKills.addAndGet(record.playerKills());
        mobKills.addAndGet(record.mobKills());
        blocksBroken.addAndGet(record.blocksBroken());
        blocksPlaced.addAndGet(record.blocksPlaced());
        advancements.addAndGet(record.advancements());
        distanceWalked.add(record.distanceWalked());
    }

    public boolean hasDeltas(ActivityDailyRecord record) {
        return record.playtimeSeconds() != 0
                || record.joins() != 0
                || record.commandsUsed() != 0
                || record.deaths() != 0
                || record.playerKills() != 0
                || record.mobKills() != 0
                || record.blocksBroken() != 0
                || record.blocksPlaced() != 0
                || record.advancements() != 0
                || record.distanceWalked() > 0;
    }
}
