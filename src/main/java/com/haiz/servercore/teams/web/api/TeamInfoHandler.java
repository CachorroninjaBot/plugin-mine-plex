package com.haiz.servercore.teams.web.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.UUID;

public final class TeamInfoHandler implements HttpHandler {

    private final TeamsModule module;

    public TeamInfoHandler(TeamsModule module) {
        this.module = module;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ApiUtils.handleOptions(exchange)) return;
        ApiUtils.addCorsHeaders(exchange);

        if (!"GET".equals(exchange.getRequestMethod())) {
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

        JsonObject resp = new JsonObject();
        resp.addProperty("id", bridge.getTeamId(team).toString());
        resp.addProperty("name", bridge.getTeamName(team));
        resp.addProperty("tag", bridge.getTeamTag(team));
        resp.addProperty("description", bridge.getTeamDescription(team));
        resp.addProperty("color", bridge.getTeamColor(team));
        resp.addProperty("open", bridge.isTeamOpen(team));
        resp.addProperty("pvp", bridge.isTeamPvp(team));
        resp.addProperty("level", bridge.getTeamLevel(team));
        resp.addProperty("score", bridge.getTeamScore(team));
        resp.addProperty("money", bridge.getTeamMoney(team));

        Object home = bridge.getTeamHome(team);
        if (home != null) {
            JsonObject homeJson = new JsonObject();
            homeJson.addProperty("world", ((org.bukkit.Location) home).getWorld().getName());
            homeJson.addProperty("x", ((org.bukkit.Location) home).getX());
            homeJson.addProperty("y", ((org.bukkit.Location) home).getY());
            homeJson.addProperty("z", ((org.bukkit.Location) home).getZ());
            resp.add("home", homeJson);
        }

        resp.addProperty("memberCount", bridge.getMembers(team).size());
        resp.addProperty("warpCount", bridge.getWarps(team).size());
        resp.addProperty("allyCount", bridge.getAllies(team).size());
        resp.addProperty("isOwner", bridge.isOwner(team, playerUUID));
        resp.addProperty("isAdmin", bridge.isAdmin(team, playerUUID));

        ApiUtils.sendJson(exchange, 200, resp);
    }
}
