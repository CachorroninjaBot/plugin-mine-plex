package com.haiz.servercore.activity;

import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class ActivityDiscordService {
    private final JavaPlugin plugin;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private volatile ActivityConfig config;

    public ActivityDiscordService(JavaPlugin plugin, ActivityConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void reload(ActivityConfig config) {
        this.config = config;
    }

    public boolean isConfigured() {
        String url = config.webhookUrl();
        return config.discordEnabled() && url != null && !url.isBlank() && !url.equalsIgnoreCase("COLOQUE_AQUI");
    }

    public CompletableFuture<Boolean> send(ActivityEmbed embed) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(false);
        }
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(config.webhookUrl().trim()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(embed.toJson()))
                    .build();
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("[HaizActivity] Webhook invalido: " + exception.getMessage());
            return CompletableFuture.completedFuture(false);
        }
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(response -> {
                    boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
                    if (!success) {
                        plugin.getLogger().warning("[HaizActivity] Discord respondeu HTTP " + response.statusCode() + ".");
                    }
                    return success;
                })
                .exceptionally(error -> {
                    plugin.getLogger().warning("[HaizActivity] Falha no webhook: " + error.getMessage());
                    return false;
                });
    }

    public CompletableFuture<Boolean> sendTest() {
        return send(new ActivityEmbed(
                "Haiz Activity conectado",
                "O webhook recebeu o embed de teste com sucesso.",
                0x2ECC71
        ).addField("Storage", "SQLite", true)
                .addField("Modulo", "Ativo", true));
    }
}
