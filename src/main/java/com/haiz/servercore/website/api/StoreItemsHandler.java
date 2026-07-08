package com.haiz.servercore.website.api;

import com.haiz.servercore.website.StoreItems;
import com.haiz.servercore.website.WebsiteModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StoreItemsHandler implements HttpHandler {

    private final WebsiteModule module;

    public StoreItemsHandler(WebsiteModule module) { this.module = module; }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ApiUtils.handleCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ApiUtils.sendError(exchange, 405, "Metodo nao permitido");
            return;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> pix = new LinkedHashMap<>();
        StoreItems.PIX.forEach((id, item) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", item.name());
            m.put("price", item.price());
            pix.put(id, m);
        });
        data.put("pix", pix);

        Map<String, Object> mobcoins = new LinkedHashMap<>();
        StoreItems.MOBCOINS.forEach((id, item) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", item.name());
            m.put("cost", item.price());
            mobcoins.put(id, m);
        });
        data.put("mobcoins", mobcoins);

        ApiUtils.sendJson(exchange, 200, data);
    }
}
