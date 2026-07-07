package com.haiz.servercore.discord.commandlog;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class CommandLogConfig {
    private final File configFile;
    private FileConfiguration config;

    public CommandLogConfig(File dataFolder) {
        this.configFile = new File(dataFolder, "config/command-logs.yml");
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try (InputStream in = getClass().getResourceAsStream("/config/command-logs.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        load();
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", false);
    }

    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }

    public int getWaitTicks() {
        return config.getInt("wait-ticks-after-execute", 1);
    }

    // Webhook URLs
    public String getExecutedWebhook() {
        return config.getString("webhooks.executed", "");
    }

    public String getNoPermissionWebhook() {
        return config.getString("webhooks.no-permission", "");
    }

    public String getUnknownCommandWebhook() {
        return config.getString("webhooks.unknown-command", "");
    }

    // Embed defaults
    public String getEmbedAuthorName() {
        return config.getString("embed-defaults.author-name", "Command Logs");
    }

    public int getEmbedDefaultColor() {
        return config.getInt("embed-defaults.color", 5814783);
    }

    public String getEmbedFooterText() {
        return config.getString("embed-defaults.footer-text", "Server Logs");
    }

    public boolean isEmbedTimestampEnabled() {
        return config.getBoolean("embed-defaults.include-timestamp", true);
    }

    public String getEmbedFooterIconUrl() {
        return config.getString("embed-defaults.footer-icon-url", "");
    }

    // Template getters
    public String getTemplateTitle(String template) {
        return config.getString("templates." + template + ".title", "");
    }

    public String getTemplateDescription(String template) {
        return config.getString("templates." + template + ".description", "");
    }

    public int getTemplateColor(String template) {
        return config.getInt("templates." + template + ".color", getEmbedDefaultColor());
    }

    public String getTemplateFooter(String template) {
        return config.getString("templates." + template + ".footer", getEmbedFooterText());
    }

    public boolean isTemplateTimestampEnabled(String template) {
        return config.getBoolean("templates." + template + ".include-timestamp", isEmbedTimestampEnabled());
    }

    public boolean isTemplateImageEnabled(String template) {
        return config.getBoolean("templates." + template + ".include-image", false);
    }

    public String getTemplateImageUrl(String template) {
        return config.getString("templates." + template + ".image-url", "");
    }

    public boolean isTemplateThumbnailEnabled(String template) {
        return config.getBoolean("templates." + template + ".include-thumbnail", false);
    }

    public String getTemplateThumbnailUrl(String template) {
        return config.getString("templates." + template + ".thumbnail-url", "");
    }
}
