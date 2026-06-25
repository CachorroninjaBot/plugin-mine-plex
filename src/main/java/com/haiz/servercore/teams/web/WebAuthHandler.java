package com.haiz.servercore.teams.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.haiz.servercore.teams.TeamsModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WebAuthHandler implements HttpHandler {

    private static final Map<String, AuthCode> PENDING_CODES = new ConcurrentHashMap<>();
    private static final Map<String, UUID> AUTH_TOKENS = new ConcurrentHashMap<>();
    private final TeamsModule module;
    private final Gson gson = new Gson();

    public WebAuthHandler(TeamsModule module) {
        this.module = module;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, 0);
            exchange.getResponseBody().close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/link")) {
            handleLinkRequest(exchange);
        } else if (path.endsWith("/verify")) {
            handleVerify(exchange);
        } else {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
        }
    }

    private void handleLinkRequest(HttpExchange exchange) throws IOException {
        String code = generateCode();
        PENDING_CODES.put(code, new AuthCode(code, System.currentTimeMillis()));

        JsonObject resp = new JsonObject();
        resp.addProperty("code", code);
        resp.addProperty("message", "Use /time web " + code + " no Minecraft");
        resp.addProperty("expiresIn", module.teamsConfig().authCodeExpiry());

        sendJson(exchange, 200, resp);
    }

    private void handleVerify(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Método não permitido");
            return;
        }

        JsonObject body = gson.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), JsonObject.class);
        String code = body.get("code").getAsString();

        AuthCode authCode = PENDING_CODES.get(code);
        if (authCode == null) {
            sendError(exchange, 400, "Código inválido ou expirado");
            return;
        }

        long elapsed = (System.currentTimeMillis() - authCode.createdAt) / 1000;
        if (elapsed > module.teamsConfig().authCodeExpiry()) {
            PENDING_CODES.remove(code);
            sendError(exchange, 400, "Código expirado");
            return;
        }

        if (!authCode.verified) {
            sendError(exchange, 400, "Código ainda não confirmado no Minecraft");
            return;
        }

        PENDING_CODES.remove(code);
        String token = UUID.randomUUID().toString();
        AUTH_TOKENS.put(token, authCode.playerUUID);

        JsonObject resp = new JsonObject();
        resp.addProperty("token", token);
        resp.addProperty("uuid", authCode.playerUUID.toString());
        sendJson(exchange, 200, resp);
    }

    public static boolean verifyCode(String code, UUID playerUUID) {
        AuthCode authCode = PENDING_CODES.get(code);
        if (authCode == null) return false;
        long elapsed = (System.currentTimeMillis() - authCode.createdAt) / 1000;
        if (elapsed > 300) {
            PENDING_CODES.remove(code);
            return false;
        }
        authCode.verified = true;
        authCode.playerUUID = playerUUID;
        return true;
    }

    public static UUID resolveToken(String token) {
        return AUTH_TOKENS.get(token);
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    private void sendJson(HttpExchange exchange, int status, JsonObject json) throws IOException {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        JsonObject resp = new JsonObject();
        resp.addProperty("error", message);
        sendJson(exchange, status, resp);
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static class AuthCode {
        final String code;
        final long createdAt;
        boolean verified;
        UUID playerUUID;

        AuthCode(String code, long createdAt) {
            this.code = code;
            this.createdAt = createdAt;
        }
    }
}
