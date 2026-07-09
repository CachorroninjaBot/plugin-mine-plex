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
    private final String pluginSecret;
    private final Gson gson = new Gson();
    private int taskId = -1;
    private MobCoinsDatabase mobCoinsDb;

    public StorePoller(JavaPlugin plugin, String apiUrl, String pluginSecret) {
        this.plugin = plugin;
        this.apiUrl = apiUrl;
        this.pluginSecret = pluginSecret;
    }

    public void setMobCoinsDb(MobCoinsDatabase mobCoinsDb) {
        this.mobCoinsDb = mobCoinsDb;
    }

    public void start() {
        if (taskId != -1) return;

        plugin.getLogger().info("[Store] Iniciando Poller...");
        plugin.getLogger().info("[Store] API URL: " + apiUrl);

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                pollPendingPurchases();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 5, 20L * 5).getTaskId(); // 5 seconds

        plugin.getLogger().info("[Store] Poller iniciado! Verificando compras pendentes a cada 5s.");
        plugin.getLogger().info("[Store] Primeira verificação em 5 segundos...");
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
            plugin.getLogger().info("[Store] Verificando compras pendentes em: " + url);

            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("X-Plugin-Secret", pluginSecret);

            int responseCode = conn.getResponseCode();
            plugin.getLogger().info("[Store] Resposta da API: " + responseCode);

            if (responseCode != 200) {
                plugin.getLogger().warning("[Store] API retornou código: " + responseCode);
                conn.disconnect();
                return;
            }

            JsonObject response;
            try (var reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                response = gson.fromJson(reader, JsonObject.class);
            }
            conn.disconnect();

            JsonArray pending = response.getAsJsonArray("pending");
            int count = pending != null ? pending.size() : 0;
            plugin.getLogger().info("[Store] Compras pendentes encontradas: " + count);

            if (pending == null || pending.isEmpty()) return;

            for (JsonElement elem : pending) {
                JsonObject purchase = elem.getAsJsonObject();
                processPurchase(purchase);
            }

            // Also sync MobCoins balances
            syncMobCoinsBalances();

        } catch (java.net.ConnectException e) {
            plugin.getLogger().warning("[Store] Não foi possível conectar à API: " + apiUrl);
            plugin.getLogger().warning("[Store] Erro: " + e.getMessage());
        } catch (java.net.SocketTimeoutException e) {
            plugin.getLogger().warning("[Store] Timeout ao conectar à API: " + apiUrl);
        } catch (Exception e) {
            plugin.getLogger().warning("[Store] Polling error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void processPurchase(JsonObject purchase) {
        String id = purchase.get("id").getAsString();
        String playerName = purchase.get("player_name").getAsString();
        String itemId = purchase.get("item_id").getAsString();
        String type = purchase.has("type") ? purchase.get("type").getAsString() : "pix";

        plugin.getLogger().info("[Store] Processando compra: id=" + id + ", player=" + playerName + ", item=" + itemId + ", type=" + type);

        StoreItems.Item item;
        if ("mobcoins".equals(type)) {
            item = StoreItems.getMobCoins(itemId);
        } else {
            item = StoreItems.getPix(itemId);
        }

        if (item == null) {
            plugin.getLogger().warning("[Store] Item desconhecido no catálogo: " + itemId);
            plugin.getLogger().warning("[Store] Itens Pix disponíveis: " + StoreItems.PIX.keySet());
            plugin.getLogger().warning("[Store] Itens MobCoins disponíveis: " + StoreItems.MOBCOINS.keySet());
            return;
        }

        String command = item.command().replace("%player%", playerName);
        plugin.getLogger().info("[Store] Executando comando: " + command);

        // Execute command on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                plugin.getLogger().info("[Store] ✅ Entregue " + item.name() + " para " + playerName);
            } catch (Exception e) {
                plugin.getLogger().severe("[Store] ❌ Erro ao executar comando: " + e.getMessage());
            }

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
            conn.setRequestProperty("X-Plugin-Secret", pluginSecret);
            conn.setDoOutput(true);

            String body = "{\"type\":\"" + type + "\"}";
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

            conn.getResponseCode(); // Execute request
            conn.disconnect();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Store] Erro ao marcar como entregue: " + e.getMessage());
        }
    }

    private void syncMobCoinsBalances() {
        if (mobCoinsDb == null) return;

        try {
            // Get all online players' balances
            var onlinePlayers = Bukkit.getOnlinePlayers();
            if (onlinePlayers.isEmpty()) return;

            StringBuilder balancesJson = new StringBuilder("[");
            boolean first = true;
            for (var player : onlinePlayers) {
                double balance = mobCoinsDb.getBalance(player.getName());
                if (!first) balancesJson.append(",");
                balancesJson.append("{\"name\":\"").append(player.getName()).append("\",\"balance\":").append(balance).append("}");
                first = false;
            }
            balancesJson.append("]");

            String url = apiUrl + "/api/mobcoins/sync";
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Plugin-Secret", pluginSecret);
            conn.setDoOutput(true);

            String body = "{\"balances\":" + balancesJson.toString() + "}";
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

            int code = conn.getResponseCode();
            if (code == 200) {
                plugin.getLogger().fine("[MobCoins] Saldos sincronizados: " + onlinePlayers.size() + " jogadores");
            }
            conn.disconnect();
        } catch (Exception e) {
            plugin.getLogger().fine("[MobCoins] Erro ao sincronizar saldos: " + e.getMessage());
        }
    }
}
