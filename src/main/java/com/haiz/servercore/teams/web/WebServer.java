package com.haiz.servercore.teams.web;

import com.haiz.servercore.teams.TeamsModule;
import com.haiz.servercore.teams.web.api.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class WebServer {

    private final TeamsModule module;
    private HttpServer server;

    public WebServer(TeamsModule module) {
        this.module = module;
    }

    public void start() {
        try {
            String host = module.teamsConfig().webHost();
            int port = module.teamsConfig().webPort();
            server = HttpServer.create(new InetSocketAddress(host.equals("0.0.0.0") ? null : host, port), 0);
            server.setExecutor(Executors.newFixedThreadPool(4));

            server.createContext("/api/auth/link", new WebAuthHandler(module));
            server.createContext("/api/auth/verify", new WebAuthHandler(module));
            server.createContext("/api/team/info", new TeamInfoHandler(module));
            server.createContext("/api/team/members", new TeamMembersHandler(module));
            server.createContext("/api/team/settings", new TeamSettingsHandler(module));
            server.createContext("/api/team/warps", new TeamWarpsHandler(module));
            server.createContext("/api/team/allies", new TeamAlliesHandler(module));
            server.createContext("/api/team/bank", new TeamBankHandler(module));

            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/") || path.equals("/index.html")) {
                    serveFile(exchange, "/web/index.html", "text/html");
                } else if (path.equals("/style.css")) {
                    serveFile(exchange, "/web/style.css", "text/css");
                } else if (path.equals("/app.js")) {
                    serveFile(exchange, "/web/app.js", "application/javascript");
                } else {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                }
            });

            server.start();
            module.plugin().getLogger().info("[Teams/Web] Servidor HTTP iniciado em " + host + ":" + port);
        } catch (IOException e) {
            module.plugin().getLogger().warning("[Teams/Web] Falha ao iniciar servidor HTTP: " + e.getMessage());
        }
    }

    private void serveFile(com.sun.net.httpserver.HttpExchange exchange, String resourcePath, String contentType) throws IOException {
        java.io.InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
            return;
        }
        byte[] bytes = is.readAllBytes();
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
