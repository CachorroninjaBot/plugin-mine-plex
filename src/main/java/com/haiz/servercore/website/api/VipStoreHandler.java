package com.haiz.servercore.website.api;

import com.haiz.servercore.website.WebsiteModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public final class VipStoreHandler implements HttpHandler {

    private final WebsiteModule module;

    public VipStoreHandler(WebsiteModule module) {
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

        ApiUtils.sendJson(exchange, 200, module.getVipStoreItems());
    }
}
