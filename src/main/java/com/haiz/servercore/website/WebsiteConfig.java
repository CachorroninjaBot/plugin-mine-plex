package com.haiz.servercore.website;

import org.bukkit.configuration.file.FileConfiguration;

public final class WebsiteConfig {

    private final FileConfiguration config;

    public WebsiteConfig(FileConfiguration config) {
        this.config = config;
    }

    public boolean enabled() {
        return config.getBoolean("enabled", true);
    }

    public String host() {
        return config.getString("host", "0.0.0.0");
    }

    public int port() {
        return config.getInt("port", 8080);
    }

    public String apiKey() {
        return config.getString("api-key", "");
    }

    public boolean corsEnabled() {
        return config.getBoolean("cors-enabled", true);
    }

    public String corsOrigin() {
        return config.getString("cors-origin", "*");
    }

    public String storeApiUrl() {
        return config.getString("store-api-url", "");
    }
}
