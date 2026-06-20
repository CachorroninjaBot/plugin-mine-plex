package com.haiz.servercore.activity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class ActivityStatsService {
    private final ActivityStorage storage;

    public ActivityStatsService(ActivityStorage storage) {
        this.storage = storage;
    }

    public List<ActivityTopEntry> topToday(String column, int limit) {
        String today = LocalDate.now().toString();
        return storage.top(today, today, column, limit);
    }

    public List<ActivityTopEntry> topWeek(String column, int limit) {
        LocalDate today = LocalDate.now();
        return storage.top(today.minusDays(6).toString(), today.toString(), column, limit);
    }

    public ActivityPeriodSummary today() {
        String today = LocalDate.now().toString();
        return storage.summary(today, today);
    }

    public ActivityPeriodSummary yesterday() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        return storage.summary(yesterday, yesterday);
    }

    public ActivityPeriodSummary week() {
        LocalDate today = LocalDate.now();
        return storage.summary(today.minusDays(6).toString(), today.toString());
    }

    public ActivityPeriodSummary previousWeek() {
        LocalDate today = LocalDate.now();
        return storage.summary(today.minusDays(13).toString(), today.minusDays(7).toString());
    }

    public Optional<ActivityPlayerProfile> player(String name) {
        return storage.playerProfile(name, LocalDate.now().toString());
    }

    public double retention(String fromDate, String toDate) {
        return storage.newPlayerRetention(fromDate, toDate, 600);
    }

    public String busiestDay(String fromDate, String toDate) {
        return storage.busiestDay(fromDate, toDate);
    }

    public double percentChange(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0 : 100;
        }
        return (current - previous) * 100.0 / previous;
    }
}
