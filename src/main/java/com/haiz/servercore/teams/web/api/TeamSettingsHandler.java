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

public final class TeamSettingsHandler implements HttpHandler {

    private final TeamsModule module;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    public TeamSettingsHandler(TeamsModule module) {
        this.module = module;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ApiUtils.handleOptions(exchange)) return;
        ApiUtils.addCorsHeaders(exchange);

        if (!"POST".equals(exchange.getRequestMethod())) {
            ApiUtils.sendError(exchange, 405, "Método não permitido");
            return;
        }

        UUID playerUUID = ApiUtils.authenticate(exchange);
        if (playerUUID == null) return;

        TeamsBridge bridge = module.bridge();
        Object team = bridge.getTeam(playerUUID);
        if (team == null) {
            ApiUtils.sendError(exchange, 404, "Você não pertence a nenhum time");
            return;
        }

        if (!bridge.isOwner(team, playerUUID)) {
            ApiUtils.sendError(exchange, 403, "Apenas o dono pode alterar configurações");
            return;
        }

        JsonObject body = gson.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), JsonObject.class);
        String setting = body.get("setting").getAsString();
        String value = body.get("value").getAsString();

        JsonObject resp = new JsonObject();
        boolean success = false;

        switch (setting) {
            case "name" -> success = bridge.setTeamName(team, value);
            case "tag" -> success = bridge.setTeamTag(team, value);
            case "description" -> success = bridge.setTeamDescription(team, value);
            case "color" -> success = bridge.setTeamColor(team, value);
            case "open" -> success = bridge.setTeamOpen(team, Boolean.parseBoolean(value));
            case "pvp" -> success = bridge.setTeamPvp(team, Boolean.parseBoolean(value));
            default -> {
                ApiUtils.sendError(exchange, 400, "Configuração desconhecida: " + setting);
                return;
            }
        }

        resp.addProperty("success", success);
        resp.addProperty("setting", setting);
        resp.addProperty("value", value);
        ApiUtils.sendJson(exchange, 200, resp);
    }
}
