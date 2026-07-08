package com.haiz.servercore.website.api;

import com.haiz.servercore.website.WebsiteModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public final class TopPlayersHandler implements HttpHandler {

    private final WebsiteModule module;

    public TopPlayersHandler(WebsiteModule module) {
        this.module = module;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ApiUtils.handleCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return;

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ApiUtils.sendError(exchange, 405, "Método não permitido");
            return;
        }

        Map<String, String> params = parseQuery(exchange.getRequestURI());
        String sortBy = params.getOrDefault("sort", "playtime");
        int limit = Math.min(Integer.parseInt(params.getOrDefault("limit", "10")), 50);

        ApiUtils.sendJson(exchange, 200, module.getTopPlayers(sortBy, limit));
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    params.put(pair[0], pair[1]);
                }
            }
        }
        return params;
    }
}
