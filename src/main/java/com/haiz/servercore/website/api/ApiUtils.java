package com.haiz.servercore.website.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class ApiUtils {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void sendJson(HttpExchange exchange, int code, Object data) throws IOException {
        byte[] response = GSON.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    public static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        sendJson(exchange, code, new ErrorResponse(message));
    }

    public static void handleCors(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1);
            exchange.getResponseBody().close();
        }
    }

    public static void handleCors(HttpExchange exchange, String allowedOrigin) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowedOrigin);
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1);
            exchange.getResponseBody().close();
        }
    }

    public static <T> T readBody(HttpExchange exchange, Class<T> type) throws IOException {
        try (var reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        }
    }

    public static String getClientIp(HttpExchange exchange) {
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    public static Gson gson() { return GSON; }

    private record ErrorResponse(String error) {}
}
