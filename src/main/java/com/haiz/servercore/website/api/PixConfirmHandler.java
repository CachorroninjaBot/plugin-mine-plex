package com.haiz.servercore.website.api;

import com.haiz.servercore.website.StoreItems;
import com.haiz.servercore.website.WebsiteModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.Map;

public final class PixConfirmHandler implements HttpHandler {

    private final WebsiteModule module;

    public PixConfirmHandler(WebsiteModule module) { this.module = module; }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ApiUtils.handleCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
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

        String status = (String) purchase.get("status");
        if (!"pending".equals(status)) {
            Map<String, Object> resp = new java.util.LinkedHashMap<>();
            resp.put("status", status);
            resp.put("message", "Pagamento ja processado");
            ApiUtils.sendJson(exchange, 200, resp);
            return;
        }

        module.getStoreStorage().updatePurchaseStatus(purchaseId, "paid");

        String itemId = (String) purchase.get("item_id");
        String playerName = (String) purchase.get("player_name");
        StoreItems.Item item = StoreItems.getPix(itemId);

        if (item != null) {
            String command = item.command().replace("%player%", playerName);
            Bukkit.getScheduler().runTask(module.plugin(), () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                module.getStoreStorage().updatePurchaseStatus(purchaseId, "delivered");
                module.plugin().getLogger().info("[Store] Entregue " + item.name() + " para " + playerName);
            });
        }

        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("status", "paid");
        resp.put("message", "Pagamento confirmado! Entrega em processamento.");
        ApiUtils.sendJson(exchange, 200, resp);
    }
}
