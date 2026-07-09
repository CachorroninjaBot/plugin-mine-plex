package com.haiz.servercore.website;

import com.haiz.servercore.storage.SQLiteDatabase;
import com.haiz.servercore.vip.LinkStorage;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;

/**
 * API HTTP mínima do plugin, usada pela minepex-api para descobrir o
 * Minecraft vinculado a um discord_id (fluxo de login com Discord no site).
 *
 * Endpoint:
 *   GET /link/discord/:discordId   (header X-Plugin-Secret obrigatório)
 *   → { "linked": bool, "uuid": str|null, "mcName": str|null }
 *
 * Requer que o Render consiga alcançar host:port neste servidor
 * (firewall/VPS com IP público). Se indisponível, o login Discord ainda
 * funciona — apenas não traz o MC automaticamente.
 */
public final class LinkApiServer {

    private final JavaPlugin plugin;
    private final LinkStorage linkStorage;
    private final String host;
    private final int port;
    private final String pluginSecret;
    private HttpServer server;

    public LinkApiServer(JavaPlugin plugin, LinkStorage linkStorage,
                         String host, int port, String pluginSecret) {
        this.plugin = plugin;
        this.linkStorage = linkStorage;
        this.host = host;
        this.port = port;
        this.pluginSecret = pluginSecret;
    }

    public void start() {
        if (port <= 0) {
            plugin.getLogger().info("[LinkAPI] Desabilitada (porta ≤ 0). Login Discord não trará MC automático.");
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext("/link/discord/", new LinkHandler());
            server.setExecutor(null);
            server.start();
            plugin.getLogger().info("[LinkAPI] Servidor de vínculo ouvindo em " + host + ":" + port);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[LinkAPI] Falha ao iniciar: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private final class LinkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            try {
                if (!"GET".equals(ex.getRequestMethod())) {
                    send(ex, 405, "{\"error\":\"method\"}");
                    return;
                }
                String secret = ex.getRequestHeaders().getFirst("X-Plugin-Secret");
                if (secret == null || !secret.equals(pluginSecret)) {
                    send(ex, 401, "{\"error\":\"unauthorized\"}");
                    return;
                }

                String path = ex.getRequestURI().getPath(); // /link/discord/:id
                String discordId = path.substring(path.lastIndexOf('/') + 1).trim();
                if (discordId.isEmpty()) {
                    send(ex, 400, "{\"error\":\"missing id\"}");
                    return;
                }

                boolean linked = linkStorage.isLinked(discordId);
                String uuid = linkStorage.uuidByDiscordId(discordId)
                        .map(UUID::toString).orElse(null);
                String mcName = linkStorage.mcNameByDiscordId(discordId).orElse(null);

                String body = String.format(
                        "{\"linked\":%s,\"uuid\":%s,\"mcName\":%s}",
                        linked,
                        uuid == null ? "null" : "\"" + uuid + "\"",
                        mcName == null ? "null" : "\"" + mcName + "\"");
                send(ex, 200, body);
            } catch (Exception e) {
                send(ex, 500, "{\"error\":\"internal\"}");
            } finally {
                ex.close();
            }
        }

        private void send(HttpExchange ex, int code, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
