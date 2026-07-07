package com.haiz.servercore.teams.web.api;

import com.google.gson.JsonObject;
import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class TeamBankHandler implements HttpHandler {

    private final TeamsModule module;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    public TeamBankHandler(TeamsModule module) {
        this.module = module;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ApiUtils.handleOptions(exchange)) return;
        ApiUtils.addCorsHeaders(exchange);

        UUID playerUUID = ApiUtils.authenticate(exchange);
        if (playerUUID == null) return;

        TeamsBridge bridge = module.bridge();
        Object team = bridge.getTeam(playerUUID);
        if (team == null) {
            ApiUtils.sendError(exchange, 404, "Você não pertence a nenhum time");
            return;
        }

        String method = exchange.getRequestMethod();
        if ("GET".equals(method)) {
            JsonObject resp = new JsonObject();
            resp.addProperty("balance", bridge.getTeamBalance(team));
            resp.addProperty("score", bridge.getTeamScore(team));
            resp.addProperty("level", bridge.getTeamLevel(team));
            ApiUtils.sendJson(exchange, 200, resp);
        } else if ("POST".equals(method)) {
            handleBankAction(exchange, team, bridge, playerUUID);
        } else {
            ApiUtils.sendError(exchange, 405, "Método não permitido");
        }
    }

    private void handleBankAction(HttpExchange exchange, Object team, TeamsBridge bridge, UUID playerUUID) throws IOException {
        JsonObject body = gson.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), JsonObject.class);
        String action = body.get("action").getAsString();
        double amount = body.get("amount").getAsDouble();

        if (amount <= 0) {
            ApiUtils.sendError(exchange, 400, "Valor inválido");
            return;
        }

        boolean success = switch (action) {
            case "deposit" -> bridge.depositMoney(team, amount);
            case "withdraw" -> {
                if (!bridge.isOwner(team, playerUUID) && !bridge.isAdmin(team, playerUUID)) {
                    ApiUtils.sendError(exchange, 403, "Sem permissão para sacar");
                    yield false;
                }
                yield bridge.withdrawMoney(team, amount);
            }
            case "rankup" -> {
                if (!bridge.isOwner(team, playerUUID)) {
                    ApiUtils.sendError(exchange, 403, "Apenas o dono pode fazer rankup");
                    yield false;
                }
                yield bridge.levelupTeam(team);
            }
            default -> {
                ApiUtils.sendError(exchange, 400, "Ação desconhecida");
                yield false;
            }
        };

        JsonObject resp = new JsonObject();
        resp.addProperty("success", success);
        resp.addProperty("balance", bridge.getTeamBalance(team));
        ApiUtils.sendJson(exchange, 200, resp);
    }
}
