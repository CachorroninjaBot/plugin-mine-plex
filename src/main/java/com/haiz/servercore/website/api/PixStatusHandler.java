package com.haiz.servercore.website.api;

import com.haiz.servercore.website.WebsiteModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public final class PixStatusHandler implements HttpHandler {

    private final WebsiteModule module;

    public PixStatusHandler(WebsiteModule module) { this.module = module; }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ApiUtils.handleCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ApiUtils.sendError(exchange, 405, "Metodo nao permitido");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String purchaseId = path.substring(path.lastIndexOf('/') + 1);
        if (purchaseId.isEmpty()) {
            ApiUtils.sendError(exchange, 400, "ID da compra obrigatorio");
            return;
        }

        Map<String, Object> purchase = module.getStoreStorage().getPurchase(purchaseId);
        if (purchase == null) {
            ApiUtils.sendError(exchange, 404, "Compra nao encontrada");
            return;
        }

        ApiUtils.sendJson(exchange, 200, purchase);
    }
}
