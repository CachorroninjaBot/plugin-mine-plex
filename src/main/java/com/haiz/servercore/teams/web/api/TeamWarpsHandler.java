package com.haiz.servercore.teams.web.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Location;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public final class TeamWarpsHandler implements HttpHandler {

    private final TeamsModule module;

    public TeamWarpsHandler(TeamsModule module) {
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

        if ("GET".equals(exchange.getRequestMethod())) {
            handleGetWarps(exchange, team, bridge);
        } else if ("DELETE".equals(exchange.getRequestMethod())) {
            handleDeleteWarp(exchange, team, bridge, playerUUID);
        } else {
            ApiUtils.sendError(exchange, 405, "Método não permitido");
        }
    }

    private void handleGetWarps(HttpExchange exchange, Object team, TeamsBridge bridge) throws IOException {
        JsonObject resp = new JsonObject();
        JsonArray warps = new JsonArray();

        Map<String, Object> warpMap = bridge.getWarps(team);
        for (Map.Entry<String, Object> entry : warpMap.entrySet()) {
            Location loc = bridge.getWarpLocation(entry.getValue());
            JsonObject warpJson = new JsonObject();
            warpJson.addProperty("name", entry.getKey());
            if (loc != null && loc.getWorld() != null) {
                warpJson.addProperty("world", loc.getWorld().getName());
                warpJson.addProperty("x", loc.getX());
                warpJson.addProperty("y", loc.getY());
                warpJson.addProperty("z", loc.getZ());
            }
            warps.add(warpJson);
        }

        resp.add("warps", warps);
        ApiUtils.sendJson(exchange, 200, resp);
    }

    private void handleDeleteWarp(HttpExchange exchange, Object team, TeamsBridge bridge, UUID playerUUID) throws IOException {
        if (!bridge.isOwner(team, playerUUID) && !bridge.isAdmin(team, playerUUID)) {
            ApiUtils.sendError(exchange, 403, "Sem permissão");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length < 5) {
            ApiUtils.sendError(exchange, 400, "Nome do warp não especificado");
            return;
        }
        String warpName = parts[4];

        boolean success = bridge.deleteWarp(team, warpName);
        JsonObject resp = new JsonObject();
        resp.addProperty("success", success);
        ApiUtils.sendJson(exchange, 200, resp);
    }
}
