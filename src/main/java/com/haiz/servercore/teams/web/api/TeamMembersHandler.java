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
import java.util.UUID;

public final class TeamMembersHandler implements HttpHandler {

    private final TeamsModule module;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    public TeamMembersHandler(TeamsModule module) {
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
            handleGetMembers(exchange, team, bridge, playerUUID);
        } else if ("POST".equals(method)) {
            handleMemberAction(exchange, team, bridge, playerUUID);
        } else {
            ApiUtils.sendError(exchange, 405, "Método não permitido");
        }
    }

    private void handleGetMembers(HttpExchange exchange, Object team, TeamsBridge bridge, UUID playerUUID) throws IOException {
        JsonObject resp = new JsonObject();
        JsonArray members = new JsonArray();

        for (Object member : bridge.getMembers(team)) {
            UUID memberUUID = bridge.getPlayerUUID(member);
            JsonObject memberJson = new JsonObject();
            memberJson.addProperty("uuid", memberUUID.toString());
            memberJson.addProperty("name", bridge.resolvePlayerName(memberUUID));
            memberJson.addProperty("rank", bridge.getPlayerRank(member));
            memberJson.addProperty("rankOrdinal", bridge.getPlayerRankOrdinal(member));
            memberJson.addProperty("title", bridge.getPlayerTitle(member));
            memberJson.addProperty("isOnline", module.plugin().getServer().getPlayer(memberUUID) != null);
            members.add(memberJson);
        }

        resp.add("members", members);
        resp.addProperty("isOwner", bridge.isOwner(team, playerUUID));
        resp.addProperty("isAdmin", bridge.isAdmin(team, playerUUID));
        ApiUtils.sendJson(exchange, 200, resp);
    }

    private void handleMemberAction(HttpExchange exchange, Object team, TeamsBridge bridge, UUID playerUUID) throws IOException {
        boolean isOwner = bridge.isOwner(team, playerUUID);
        boolean isAdmin = bridge.isAdmin(team, playerUUID);

        JsonObject body = gson.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), JsonObject.class);
        String action = body.get("action").getAsString();
        UUID targetUUID = UUID.fromString(body.get("target").getAsString());

        JsonObject resp = new JsonObject();
        boolean success = false;

        switch (action) {
            case "invite" -> {
                if (!isOwner && !isAdmin) {
                    ApiUtils.sendError(exchange, 403, "Sem permissão");
                    return;
                }
                success = bridge.invitePlayer(team, targetUUID);
            }
            case "kick" -> {
                if (!isOwner && !isAdmin) {
                    ApiUtils.sendError(exchange, 403, "Sem permissão");
                    return;
                }
                success = bridge.kickPlayer(team, targetUUID);
            }
            case "promote" -> {
                if (!isOwner) {
                    ApiUtils.sendError(exchange, 403, "Apenas o dono pode promover");
                    return;
                }
                success = bridge.promotePlayer(team, targetUUID);
            }
            case "demote" -> {
                if (!isOwner) {
                    ApiUtils.sendError(exchange, 403, "Apenas o dono pode rebaixar");
                    return;
                }
                success = bridge.demotePlayer(team, targetUUID);
            }
            default -> {
                ApiUtils.sendError(exchange, 400, "Ação desconhecida: " + action);
                return;
            }
        }

        resp.addProperty("success", success);
        resp.addProperty("action", action);
        ApiUtils.sendJson(exchange, 200, resp);
    }
}
