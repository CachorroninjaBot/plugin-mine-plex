package com.haiz.servercore.website.api;

import com.haiz.servercore.website.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MobCoinsBuyHandler implements HttpHandler {

    private final WebsiteModule module;

    public MobCoinsBuyHandler(WebsiteModule module) { this.module = module; }

    private record MobCoinsRequest(String playerName, String itemId) {}

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

        MobCoinsRequest req;
        try {
            req = ApiUtils.readBody(exchange, MobCoinsRequest.class);
        } catch (Exception e) {
            ApiUtils.sendError(exchange, 400, "JSON invalido");
            return;
        }

        if (req.playerName() == null || req.itemId() == null) {
            ApiUtils.sendError(exchange, 400, "Nome do jogador e item sao obrigatorios");
            return;
        }

        String playerName = req.playerName().replaceAll("[^a-zA-Z0-9_]", "");
        if (playerName.isEmpty() || playerName.length() > 16) {
            ApiUtils.sendError(exchange, 400, "Nome do jogador invalido");
            return;
        }
        String itemId = req.itemId().replaceAll("[^a-zA-Z0-9_]", "");

        StoreItems.Item item = StoreItems.getMobCoins(itemId);
        if (item == null) {
            ApiUtils.sendError(exchange, 404, "Item nao encontrado");
            return;
        }

        // Check balance from IridiumMobCoins database
        double balance = 0;
        if (module.getMobCoinsDb() != null) {
            balance = module.getMobCoinsDb().getBalance(playerName);
        } else {
            ApiUtils.sendError(exchange, 503, "Sistema de MobCoins indisponivel");
            return;
        }

        int cost = (int) item.price();
        module.plugin().getLogger().info("[MobCoins] " + playerName + " tem " + balance + " MobCoins, custo: " + cost);

        if (balance < cost) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Saldo insuficiente");
            err.put("balance", balance);
            err.put("cost", cost);
            ApiUtils.sendJson(exchange, 400, err);
            return;
        }

        // Record purchase
        String purchaseId = UUID.randomUUID().toString();
        module.getStoreStorage().createMobCoinsPurchase(purchaseId, playerName, null, itemId, item.name(), cost);

        // Execute commands on main thread
        String removeCommand = "mc remove " + playerName + " " + cost;
        String itemCommand = item.command().replace("%player%", playerName);

        Bukkit.getScheduler().runTask(module.plugin(), () -> {
            try {
                // Deduct MobCoins
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), removeCommand);
                module.plugin().getLogger().info("[MobCoins] Deduzido " + cost + " de " + playerName);

                // Give item
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), itemCommand);
                module.plugin().getLogger().info("[Store] Entregue " + item.name() + " (MobCoins) para " + playerName);

                // Mark as delivered
                module.getStoreStorage().updateMobCoinsPurchaseStatus(purchaseId, "delivered");
            } catch (Exception e) {
                module.plugin().getLogger().severe("[MobCoins] Erro ao processar compra: " + e.getMessage());
            }
        });

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("purchaseId", purchaseId);
        resp.put("item", item.name());
        resp.put("cost", cost);
        resp.put("status", "processing");
        resp.put("message", "Compra em processamento!");

        ApiUtils.sendJson(exchange, 200, resp);
    }
}
