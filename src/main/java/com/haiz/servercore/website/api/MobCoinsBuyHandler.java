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

        String playerName = req.playerName().replaceAll("[^a-zA-Z0-9_]", "").substring(0, Math.min(req.playerName().length(), 16));
        String itemId = req.itemId().replaceAll("[^a-zA-Z0-9_]", "");

        StoreItems.Item item = StoreItems.getMobCoins(itemId);
        if (item == null) {
            ApiUtils.sendError(exchange, 404, "Item nao encontrado");
            return;
        }

        var mobCoins = module.plugin().vip().mobCoins();
        if (mobCoins == null || !mobCoins.isAvailable()) {
            ApiUtils.sendError(exchange, 503, "Sistema de MobCoins indisponivel");
            return;
        }

        var player = Bukkit.getOfflinePlayer(playerName);
        double balance = mobCoins.getBalance(player.getUniqueId());

        if (balance < item.price()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Saldo insuficiente");
            err.put("balance", balance);
            err.put("cost", (int) item.price());
            ApiUtils.sendJson(exchange, 400, err);
            return;
        }

        boolean withdrawn = mobCoins.withdraw(player.getUniqueId(), item.price());
        if (!withdrawn) {
            ApiUtils.sendError(exchange, 400, "Erro ao debitar MobCoins");
            return;
        }

        String purchaseId = UUID.randomUUID().toString();
        module.getStoreStorage().createMobCoinsPurchase(purchaseId, playerName, player.getUniqueId().toString(), itemId, item.name(), (int) item.price());

        String command = item.command().replace("%player%", playerName);
        Bukkit.getScheduler().runTask(module.plugin(), () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            module.getStoreStorage().updateMobCoinsPurchaseStatus(purchaseId, "delivered");
            module.plugin().getLogger().info("[Store] Entregue " + item.name() + " (MobCoins) para " + playerName);
        });

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("purchaseId", purchaseId);
        resp.put("item", item.name());
        resp.put("cost", (int) item.price());
        resp.put("status", "delivered");
        resp.put("message", "Compra realizada com sucesso!");

        ApiUtils.sendJson(exchange, 200, resp);
    }
}
