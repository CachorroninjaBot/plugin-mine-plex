package com.haiz.servercore.website;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class StorePoller {

    private final JavaPlugin plugin;
    private final String apiUrl;
    private final Gson gson = new Gson();
    private int taskId = -1;

    public StorePoller(JavaPlugin plugin, String apiUrl) {
        this.plugin = plugin;
        this.apiUrl = apiUrl;
    }

    public void start() {
        if (taskId != -1) return;

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                pollPendingPurchases();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 5, 20L * 5).getTaskId(); // 5 seconds

        plugin.getLogger().info("[Store] Poller iniciado. Verificando compras pendentes a cada 5s.");
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
            plugin.getLogger().info("[Store] Poller parado.");
        }
    }

    private void pollPendingPurchases() {
        try {
            String url = apiUrl + "/api/pending";
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return;
            }

            JsonObject response;
            try (var reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                response = gson.fromJson(reader, JsonObject.class);
            }
            conn.disconnect();

            JsonArray pending = response.getAsJsonArray("pending");
            if (pending == null || pending.isEmpty()) return;

            for (JsonElement elem : pending) {
                JsonObject purchase = elem.getAsJsonObject();
                processPurchase(purchase);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "[Store] Polling error: " + e.getMessage());
        }
    }

    private void processPurchase(JsonObject purchase) {
        String id = purchase.get("id").getAsString();
        String playerName = purchase.get("player_name").getAsString();
        String itemId = purchase.get("item_id").getAsString();
        String type = purchase.has("type") ? purchase.get("type").getAsString() : "pix";

        StoreItems.Item item;
        if ("mobcoins".equals(type)) {
            item = StoreItems.getMobCoins(itemId);
        } else {
            item = StoreItems.getPix(itemId);
        }

        if (item == null) {
            plugin.getLogger().warning("[Store] Item desconhecido: " + itemId);
            return;
        }

        String command = item.command().replace("%player%", playerName);

        // Execute command on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            plugin.getLogger().info("[Store] Entregue " + item.name() + " para " + playerName);

            // Mark as delivered (async)
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                markDelivered(id, type);
            });
        });
    }

    private void markDelivered(String purchaseId, String type) {
        try {
            String url = apiUrl + "/api/delivered/" + purchaseId;
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String body = "{\"type\":\"" + type + "\"}";
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

            conn.getResponseCode(); // Execute request
            conn.disconnect();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Store] Erro ao marcar como entregue: " + e.getMessage());
        }
    }
}
