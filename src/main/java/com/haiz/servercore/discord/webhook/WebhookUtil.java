package com.haiz.servercore.discord.webhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public final class WebhookUtil {

    private WebhookUtil() {}

    public static CompletableFuture<Integer> send(String webhookUrl, String username, String avatarUrl, String content) {
        return send(webhookUrl, username, avatarUrl, content, null);
    }

    public static CompletableFuture<Integer> send(String webhookUrl, String username, String avatarUrl, String content, JsonObject embed) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return CompletableFuture.completedFuture(-1);
        }

        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection conn = null;
            try {
                JsonObject json = new JsonObject();
                json.addProperty("username", username != null ? username : "HaizServerCore");
                json.addProperty("content", content != null ? content : "");

                if (avatarUrl != null && !avatarUrl.isBlank()) {
                    json.addProperty("avatar_url", avatarUrl);
                }

                if (embed != null) {
                    JsonArray embeds = new JsonArray();
                    embeds.add(embed);
                    json.add("embeds", embeds);
                }

                JsonObject allowedMentions = new JsonObject();
                allowedMentions.add("parse", JsonParser.parseString("[]").getAsJsonArray());
                json.add("allowed_mentions", allowedMentions);

                conn = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("User-Agent", "HaizServerCore/Webhook");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                try (OutputStream out = conn.getOutputStream()) {
                    out.write(json.toString().getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }

                return conn.getResponseCode();
            } catch (Exception e) {
                return -1;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    public static CompletableFuture<Integer> sendEmbed(String webhookUrl, String username, String avatarUrl,
                                                       String title, String description, String color,
                                                       String thumbnail, String footer) {
        JsonObject embed = buildEmbed(title, description, color, thumbnail, footer);
        return send(webhookUrl, username, avatarUrl, "", embed);
    }

    public static JsonObject buildEmbed(String title, String description, String color,
                                         String thumbnail, String footer) {
        JsonObject embed = new JsonObject();

        if (title != null) {
            embed.addProperty("title", title);
        }
        if (description != null) {
            embed.addProperty("description", description);
        }
        if (color != null) {
            embed.addProperty("color", parseColor(color));
        }
        if (thumbnail != null) {
            JsonObject thumbObj = new JsonObject();
            thumbObj.addProperty("url", thumbnail);
            embed.add("thumbnail", thumbObj);
        }
        if (footer != null) {
            JsonObject footerObj = new JsonObject();
            footerObj.addProperty("text", footer);
            embed.add("footer", footerObj);
        }

        embed.addProperty("timestamp", java.time.Instant.now().toString());

        return embed;
    }

    public static int parseColor(String hex) {
        if (hex == null || hex.isBlank()) return 0x5865F2;
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return (int) Long.parseLong(clean, 16);
        } catch (NumberFormatException e) {
            return 0x5865F2;
        }
    }
}
