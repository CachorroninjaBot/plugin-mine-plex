package com.haiz.servercore.website.api;

import com.haiz.servercore.website.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PixCreateHandler implements HttpHandler {

    private final WebsiteModule module;

    public PixCreateHandler(WebsiteModule module) { this.module = module; }

    private record PixRequest(String playerName, String itemId) {}

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ApiUtils.handleCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            ApiUtils.sendError(exchange, 405, "Metodo nao permitido");
            return;
        }

        String ip = ApiUtils.getClientIp(exchange);
        if (!module.getPurchaseLimiter().isAllowed(ip)) {
            ApiUtils.sendError(exchange, 429, "Muitas tentativas. Tente novamente mais tarde.");
            return;
        }

        PixRequest req;
        try {
            req = ApiUtils.readBody(exchange, PixRequest.class);
        } catch (Exception e) {
            ApiUtils.sendError(exchange, 400, "JSON invalido");
            return;
        }

        if (req.playerName() == null || req.itemId() == null) {
            ApiUtils.sendError(exchange, 400, "Nome do jogador e item sao obrigatorios");
            return;
        }

        String playerName = req.playerName().replaceAll("[^a-zA-Z0-9_]", "").substring(0, Math.min(req.playerName().length(), 16));
        String itemId = req.itemId().replaceAll("[^a-zA-Z0-9_]", "");

        StoreItems.Item item = StoreItems.getPix(itemId);
        if (item == null) {
            ApiUtils.sendError(exchange, 404, "Item nao encontrado");
            return;
        }

        String purchaseId = UUID.randomUUID().toString();
        String txid = "MPX" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String pixPayload = PixPayloadGenerator.generate(item.price(), txid, item.name());

        module.getStoreStorage().createPurchase(purchaseId, playerName, null, itemId, item.name(), item.price(), txid);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("purchaseId", purchaseId);
        response.put("item", item.name());
        response.put("price", item.price());
        response.put("pixPayload", pixPayload);
        response.put("txid", txid);
        response.put("expiresIn", 1800);

        ApiUtils.sendJson(exchange, 200, response);
    }
}
