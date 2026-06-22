package com.haiz.servercore.utils;

import java.util.Locale;

public final class TextUtils {
    private TextUtils() {}

    public static String commandRoot(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String normalized = raw.startsWith("/") ? raw.substring(1) : raw;
        return normalized.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    }

    public static String trimForDiscord(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
