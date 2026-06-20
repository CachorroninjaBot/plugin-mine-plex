package com.haiz.servercore.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private TimeUtils() {
    }

    public static long nowSeconds() {
        return Instant.now().getEpochSecond();
    }

    public static String formatDateTime(long epochSeconds) {
        if (epochSeconds <= 0) {
            return "nunca";
        }
        return DATE_TIME.format(Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()));
    }

    public static String todayKey() {
        return LocalDate.now().toString();
    }

    public static String dayKey(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    public static String humanDuration(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
