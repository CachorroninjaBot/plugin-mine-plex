package com.haiz.servercore.website;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.website.api.*;
import com.sun.net.httpserver.HttpServer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class WebsiteModule {

    private final HaizServerCore plugin;
    private HttpServer server;
    private boolean running;

    public WebsiteModule(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        WebsiteConfig config = config();
        if (!config.enabled()) {
            plugin.getLogger().info("[Website] Módulo desabilitado na config.");
            return;
        }

        try {
            String host = config.host();
            int port = config.port();
            InetSocketAddress addr = host.equals("0.0.0.0")
                    ? new InetSocketAddress(port)
                    : new InetSocketAddress(host, port);

            server = HttpServer.create(addr, 0);
            server.setExecutor(Executors.newFixedThreadPool(4));

            // API endpoints
            server.createContext("/api/server/status", new ServerStatusHandler(this));
            server.createContext("/api/server/players", new OnlinePlayersHandler(this));
            server.createContext("/api/server/top", new TopPlayersHandler(this));
            server.createContext("/api/store/vip", new VipStoreHandler(this));
            server.createContext("/api/store/mobcoins", new MobCoinsStoreHandler(this));
            server.createContext("/api/ranks", new RanksHandler(this));

            // CORS preflight
            server.createContext("/api/", exchange -> {
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                    exchange.sendResponseHeaders(204, -1);
                    exchange.getResponseBody().close();
                    return;
                }
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().close();
            });

            server.start();
            running = true;
            plugin.getLogger().info("[Website] API iniciada em " + host + ":" + port);
        } catch (IOException e) {
            plugin.getLogger().warning("[Website] Falha ao iniciar API: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            running = false;
            plugin.getLogger().info("[Website] API desabilitada.");
        }
    }

    public void reload() {
        stop();
        start();
    }

    public boolean isRunning() { return running; }
    public HaizServerCore plugin() { return plugin; }

    public WebsiteConfig config() {
        return new WebsiteConfig(plugin.config().getModuleConfig("website"));
    }

    // ── Helper methods ──────────────────────────────────────────────

    public Map<String, Object> getServerStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("online", true);
        data.put("playerCount", Bukkit.getOnlinePlayers().size());
        data.put("maxPlayers", Bukkit.getMaxPlayers());
        data.put("tps", getTPS());
        data.put("uptime", getUptime());
        data.put("version", Bukkit.getVersion());
        return data;
    }

    public List<Map<String, Object>> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream().map(p -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", p.getName());
            data.put("uuid", p.getUniqueId().toString());
            data.put("displayName", p.getDisplayName());
            data.put("level", p.getLevel());
            data.put("health", Math.round(p.getHealth()));
            data.put("world", p.getWorld().getName());
            return data;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTopPlayers(String sortBy, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
        Arrays.sort(offlinePlayers, (a, b) -> {
            switch (sortBy) {
                case "playtime":
                    return Integer.compare(
                        b.getStatistic(Statistic.PLAY_ONE_MINUTE),
                        a.getStatistic(Statistic.PLAY_ONE_MINUTE));
                case "kills":
                    return Integer.compare(
                        b.getStatistic(Statistic.PLAYER_KILLS),
                        a.getStatistic(Statistic.PLAYER_KILLS));
                case "deaths":
                    return Integer.compare(
                        b.getStatistic(Statistic.DEATHS),
                        a.getStatistic(Statistic.DEATHS));
                default:
                    return Integer.compare(
                        b.getStatistic(Statistic.PLAY_ONE_MINUTE),
                        a.getStatistic(Statistic.PLAY_ONE_MINUTE));
            }
        });

        int count = Math.min(limit, offlinePlayers.length);
        for (int i = 0; i < count; i++) {
            OfflinePlayer p = offlinePlayers[i];
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("rank", i + 1);
            data.put("name", p.getName() != null ? p.getName() : "Desconhecido");
            data.put("uuid", p.getUniqueId().toString());
            data.put("playtimeHours", p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000);
            data.put("kills", p.getStatistic(Statistic.PLAYER_KILLS));
            data.put("deaths", p.getStatistic(Statistic.DEATHS));
            result.add(data);
        }
        return result;
    }

    public Map<String, Object> getVipStoreItems() {
        Map<String, Object> data = new LinkedHashMap<>();
        var tiers = plugin.config().getModuleConfig("vip").getConfigurationSection("tiers");
        if (tiers != null) {
            Map<String, Object> items = new LinkedHashMap<>();
            for (String key : tiers.getKeys(false)) {
                var section = tiers.getConfigurationSection(key);
                if (section != null) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", section.getString("display-name", key));
                    item.put("price", section.getInt("price", 0));
                    item.put("description", section.getString("description", ""));
                    item.put("perks", section.getStringList("perks"));
                    items.put(key, item);
                }
            }
            data.put("tiers", items);
        }
        return data;
    }

    public Map<String, Object> getMobCoinsStoreItems() {
        Map<String, Object> data = new LinkedHashMap<>();
        var shopItems = plugin.config().getModuleConfig("mobcoins-shop");
        // This would read from IridiumMobCoins shop.yml
        // For now return placeholder
        data.put("items", new HashMap<>());
        return data;
    }

    public Map<String, Object> getRanksInfo() {
        Map<String, Object> data = new LinkedHashMap<>();
        var ranksConfig = plugin.config().getModuleConfig("ranks");
        // Read NotRanks config
        data.put("ranks", new HashMap<>());
        return data;
    }

    private double[] getTPS() {
        try {
            // Paper API
            return new double[]{
                Bukkit.getTPS()[0],
                Bukkit.getTPS()[1],
                Bukkit.getTPS()[2]
            };
        } catch (Exception e) {
            return new double[]{20.0, 20.0, 20.0};
        }
    }

    private long getUptime() {
        return (System.currentTimeMillis() - plugin.getServerStartTime()) / 1000;
    }
}
