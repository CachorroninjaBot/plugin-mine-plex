package com.haiz.servercore.website.api;

import com.haiz.servercore.website.WebsiteModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MobCoinsBalanceHandler implements HttpHandler {

    private final WebsiteModule module;

    public MobCoinsBalanceHandler(WebsiteModule module) { this.module = module; }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ApiUtils.handleCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ApiUtils.sendError(exchange, 405, "Metodo nao permitido");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String playerName = path.substring(path.lastIndexOf('/') + 1);
        if (playerName.isEmpty()) {
            ApiUtils.sendError(exchange, 400, "Nome do jogador obrigatorio");
            return;
        }

        double balance = 0;
        if (module.getMobCoinsDb() != null) {
            balance = module.getMobCoinsDb().getBalance(playerName);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("player", playerName);
        resp.put("balance", balance);

        ApiUtils.sendJson(exchange, 200, resp);
    }
}
