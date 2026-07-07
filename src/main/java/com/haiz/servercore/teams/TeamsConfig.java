package com.haiz.servercore.teams;

import org.bukkit.configuration.file.FileConfiguration;

public final class TeamsConfig {

    private boolean enabled;
    private boolean webEnabled;
    private int webPort;
    private String webHost;
    private int authCodeExpiry;
    private String discordPanelChannelId;
    private String guiTitle;

    public TeamsConfig(FileConfiguration config) {
        reload(config);
    }

    public void reload(FileConfiguration config) {
        this.enabled = config.getBoolean("enabled", true);
        this.webEnabled = config.getBoolean("web.enabled", true);
        this.webPort = config.getInt("web.port", 8080);
        this.webHost = config.getString("web.host", "0.0.0.0");
        this.authCodeExpiry = config.getInt("web.auth-code-expiry", 300);
        this.discordPanelChannelId = config.getString("discord.panel-channel-id", "");
        this.guiTitle = config.getString("gui.title", "§8§lGerenciar Time");
    }

    public boolean enabled() { return enabled; }
    public boolean webEnabled() { return webEnabled; }
    public int webPort() { return webPort; }
    public String webHost() { return webHost; }
    public int authCodeExpiry() { return authCodeExpiry; }
    public String discordPanelChannelId() { return discordPanelChannelId; }
    public String guiTitle() { return guiTitle; }
}
