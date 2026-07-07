package com.haiz.servercore.teams.web.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public final class TeamAlliesHandler implements HttpHandler {

    private final TeamsModule module;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    public TeamAlliesHandler(TeamsModule module) {
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
            handleGetAllies(exchange, team, bridge, playerUUID);
        } else if ("POST".equals(method)) {
            handleAllyAction(exchange, team, bridge, playerUUID);
        } else {
            ApiUtils.sendError(exchange, 405, "Método não permitido");
        }
    }

    private void handleGetAllies(HttpExchange exchange, Object team, TeamsBridge bridge, UUID playerUUID) throws IOException {
        JsonObject resp = new JsonObject();
        JsonArray allies = new JsonArray();

        Set<UUID> allyIds = bridge.getAllyIds(team);
        for (UUID allyId : allyIds) {
            JsonObject allyJson = new JsonObject();
            allyJson.addProperty("id", allyId.toString());
            allyJson.addProperty("name", bridge.getAllyName(allyId));
            allies.add(allyJson);
        }

        resp.add("allies", allies);
        resp.addProperty("isOwner", bridge.isOwner(team, playerUUID));
        ApiUtils.sendJson(exchange, 200, resp);
    }

    private void handleAllyAction(HttpExchange exchange, Object team, TeamsBridge bridge, UUID playerUUID) throws IOException {
        if (!bridge.isOwner(team, playerUUID)) {
            ApiUtils.sendError(exchange, 403, "Apenas o dono pode gerenciar alianças");
            return;
        }

        JsonObject body = gson.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), JsonObject.class);
        String action = body.get("action").getAsString();
        UUID targetId = UUID.fromString(body.get("target").getAsString());

        boolean success = switch (action) {
            case "accept" -> bridge.addAlly(team, targetId);
            case "reject", "remove" -> bridge.removeAlly(team, targetId);
            default -> {
                ApiUtils.sendError(exchange, 400, "Ação desconhecida");
                yield false;
            }
        };

        JsonObject resp = new JsonObject();
        resp.addProperty("success", success);
        ApiUtils.sendJson(exchange, 200, resp);
    }
}
