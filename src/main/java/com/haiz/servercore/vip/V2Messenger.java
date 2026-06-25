package com.haiz.servercore.vip;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Envia mensagens Components v2 via REST API do Discord.
 *
 * JDA 5.0.0 não tem suporte nativo ao Components v2, então usamos
 * o HttpClient do Java para enviar o payload raw JSON diretamente.
 */
public final class V2Messenger {

    private static final String API_BASE = "https://discord.com/api/v10";
    private static final Logger LOG = Logger.getLogger("V2Messenger");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private V2Messenger() {}

    /**
     * Envia uma mensagem Components v2 para um canal de texto.
     * Retorna um CompletableFuture que completa com o status HTTP.
     */
    public static CompletableFuture<Integer> send(TextChannel channel, DataObject message) {
        String channelId = channel.getId();
        String token = channel.getJDA().getToken();
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + "/channels/" + channelId + "/messages"))
                .header("Authorization", "Bot " + token)
                .header("Content-Type", "application/json")
                .header("User-Agent", "HaizServerCore/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(message.toString()))
                .timeout(Duration.ofSeconds(15))
                .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    if (r.statusCode() < 200 || r.statusCode() >= 300) {
                        LOG.warning("[V2] Discord retornou HTTP " + r.statusCode() + " para canal " + channelId + ": " + r.body());
                    }
                    return r.statusCode();
                })
                .exceptionally(e -> {
                    LOG.warning("[V2] Falha ao enviar para canal " + channelId + ": " + e.getMessage());
                    return -1;
                });
    }

    /**
     * Envia uma mensagem Components v2 como resposta a uma interação (followup).
     */
    public static CompletableFuture<Integer> replyInteraction(JDA jda, String interactionToken, DataObject message) {
        String appId = jda.getSelfUser().getApplicationId();
        String token = jda.getToken();
        DataObject payload = DataObject.fromJson(message.toString());
        payload.put("flags", 32768);
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + "/webhooks/" + appId + "/" + interactionToken))
                .header("Authorization", "Bot " + token)
                .header("Content-Type", "application/json")
                .header("User-Agent", "HaizServerCore/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .timeout(Duration.ofSeconds(15))
                .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    if (r.statusCode() < 200 || r.statusCode() >= 300) {
                        LOG.warning("[V2] Discord retornou HTTP " + r.statusCode() + " na interação: " + r.body());
                    }
                    return r.statusCode();
                })
                .exceptionally(e -> {
                    LOG.warning("[V2] Falha ao responder interação: " + e.getMessage());
                    return -1;
                });
    }

    /**
     * Edita uma mensagem existente com Components v2.
     */
    public static CompletableFuture<Integer> edit(TextChannel channel, String messageId, DataObject message) {
        String channelId = channel.getId();
        String token = channel.getJDA().getToken();
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + "/channels/" + channelId + "/messages/" + messageId))
                .header("Authorization", "Bot " + token)
                .header("Content-Type", "application/json")
                .header("User-Agent", "HaizServerCore/1.0")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(message.toString()))
                .timeout(Duration.ofSeconds(15))
                .build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    if (r.statusCode() < 200 || r.statusCode() >= 300) {
                        LOG.warning("[V2] Discord retornou HTTP " + r.statusCode() + " ao editar: " + r.body());
                    }
                    return r.statusCode();
                })
                .exceptionally(e -> {
                    LOG.warning("[V2] Falha ao editar mensagem: " + e.getMessage());
                    return -1;
                });
    }
}
