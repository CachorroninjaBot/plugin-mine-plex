package com.haiz.servercore.metrics;

public record ServerPerformanceStats(
        int samples,
        double averageTps,
        double minimumTps,
        double maximumMspt,
        double averageOnline,
        int peakOnline,
        long sinceSeconds,
        long untilSeconds
) {
    public static ServerPerformanceStats empty(long sinceSeconds, long untilSeconds) {
        return new ServerPerformanceStats(0, -1, -1, 0, 0, 0, sinceSeconds, untilSeconds);
    }
}
