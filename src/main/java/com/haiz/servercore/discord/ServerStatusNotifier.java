package com.haiz.servercore.discord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class ServerStatusNotifier {

    private final JavaPlugin plugin;
    private final String webhookUrl;
    private String lastMessageId;

    public ServerStatusNotifier(JavaPlugin plugin, String webhookUrl) {
        this.plugin = plugin;
        this.webhookUrl = webhookUrl;
    }

    public void sendOnlineStatus() {
        String content = "## 🟢 **Servidor está ligado.**\n\nO servidor já está online e disponível para jogar. <@&1516825406924128387>\n-# <:iconswarning:1518121765602066503> Para desativar essa notificação vá em <id:customize>";
        sendWebhookMessage(content);
    }

    public void sendOfflineStatus() {
        String content = "## 🛑 **Servidor foi desligado.**\n\nO servidor está offline no momento. Avisaremos quando ele voltar. <@&1516825406924128387>\n-# <:iconswarning:1518121765602066503> Para desativar essa notificação vá em <id:customize>";
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> executeWebhook(content));
    }

    private void sendWebhookMessage(String content) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            executeWebhook(content);
        });
    }

    private void sendWebhookMessageSync(String content) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        executeWebhook(content);
    }

    private void executeWebhook(String content) {
        try {
            deleteLastMessage();

            JsonObject json = new JsonObject();
            json.addProperty("username", "PEX LEGENDS");
            json.addProperty("content", content);

            JsonObject allowedMentions = new JsonObject();
            allowedMentions.add("parse", com.google.gson.JsonParser.parseString("[\"roles\"]").getAsJsonArray());
            json.add("allowed_mentions", allowedMentions);

            String response = http_request("POST", webhookUrl + "?wait=true", json.toString());

            if (response != null && !response.isBlank()) {
                JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                if (obj.has("id")) {
                    lastMessageId = obj.get("id").getAsString();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[ServerStatus] Falha ao enviar webhook: " + e.getMessage());
        }
    }

    private void deleteLastMessage() {
        if (lastMessageId == null || lastMessageId.isBlank()) return;
        try {
            http_request("DELETE", webhookUrl + "/messages/" + lastMessageId, null);
            lastMessageId = null;
        } catch (Exception ignored) {}
    }

    private String http_request(String method, String urlStr, String body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        try {
            conn.setRequestMethod(method);
            conn.setRequestProperty("User-Agent", "HaizServerCore");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoInput(true);

            if (body != null && !body.isBlank()) {
                conn.setDoOutput(true);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
            }

            int code = conn.getResponseCode();
            java.io.InputStream stream = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (stream == null) return "";

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } finally {
            conn.disconnect();
        }
    }
}
