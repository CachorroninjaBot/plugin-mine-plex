package com.haiz.servercore.utils;

import com.haiz.servercore.config.ConfigManager;

import java.util.Locale;

public final class TextUtils {
    private TextUtils() {
    }

    public static String sanitizeCommand(String raw, ConfigManager config) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.startsWith("/") ? raw.substring(1) : raw;
        String root = normalized.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (config.shouldHideSensitiveArguments() && config.isSensitiveCommand(root)) {
            return "/" + root + " [OCULTO]";
        }
        return raw.startsWith("/") ? raw : "/" + raw;
    }

    public static String commandRoot(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.startsWith("/") ? raw.substring(1) : raw;
        return normalized.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    }

    public static String trimForDiscord(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
