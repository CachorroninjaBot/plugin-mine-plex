package com.haiz.servercore.teams.web.api;

import com.google.gson.JsonObject;
import com.haiz.servercore.teams.web.WebAuthHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class ApiUtils {

    private ApiUtils() {}

    public static UUID authenticate(HttpExchange exchange) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            sendError(exchange, 401, "Não autenticado");
            return null;
        }
        String token = auth.substring(7);
        UUID uuid = WebAuthHandler.resolveToken(token);
        if (uuid == null) {
            sendError(exchange, 401, "Token inválido");
            return null;
        }
        return uuid;
    }

    public static void sendJson(HttpExchange exchange, int status, JsonObject json) throws IOException {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    public static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        JsonObject resp = new JsonObject();
        resp.addProperty("error", message);
        sendJson(exchange, status, resp);
    }

    public static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    public static boolean handleOptions(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, 0);
            exchange.getResponseBody().close();
            return true;
        }
        return false;
    }
}
