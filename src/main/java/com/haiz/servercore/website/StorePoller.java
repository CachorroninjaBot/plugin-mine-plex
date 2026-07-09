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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Faz o polling da minepex-api (loja externa) para:
 *   1. Entregar compras pendentes (Pix/MobCoins) no servidor.
 *   2. Popular o catálogo StoreItems a partir de /api/store/items
 *      (fonte única da verdade = store-items.json na API).
 *   3. Sincronizar saldos de MobCoins dos jogadores online.
 *
 * Autônomo: não depende da WebsiteModule (API embutida removida).
 */
public final class StorePoller {

    private final JavaPlugin plugin;
    private final String apiUrl;
    private final String pluginSecret;
    private final Gson gson = new Gson();
    private final MobCoinsDatabase mobCoinsDb;
    private int taskId = -1;

    public StorePoller(JavaPlugin plugin, String apiUrl, String pluginSecret) {
        this.plugin = plugin;
        this.apiUrl = apiUrl;
        this.pluginSecret = pluginSecret;
        this.mobCoinsDb = new MobCoinsDatabase(plugin);
    }

    public MobCoinsDatabase getMobCoinsDb() { return mobCoinsDb; }

    public void start() {
        if (taskId != -1) return;

        plugin.getLogger().info("[Store] Iniciando Poller...");
        plugin.getLogger().info("[Store] API URL: " + apiUrl);

        // Popula o catálogo imediatamente (e depois a cada ciclo)
        refreshCatalog();

        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                refreshCatalog();
                pollPendingPurchases();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 5, 20L * 5).getTaskId(); // 5s

        // Sync de saldos em timer separado (menos frequente) para não sobrecarregar a API
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> syncMobCoinsBalances(), 20L * 30, 20L * 30); // 30s

        plugin.getLogger().info("[Store] Poller iniciado! Catálogo + compras a cada 5s, sync MobCoins a cada 30s.");
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (mobCoinsDb != null) mobCoinsDb.close();
        plugin.getLogger().info("[Store] Poller parado.");
    }

    // ── Catálogo (fonte única: minepex-api /api/store/items) ───────────────
    @SuppressWarnings("unchecked")
    private void refreshCatalog() {
        try {
            JsonObject resp = getJson(apiUrl + "/api/store/items");
            if (resp == null) return;

            Map<String, Map<String, Object>> pix = toMap(resp.get("pix"));
            Map<String, Map<String, Object>> mc = toMap(resp.get("mobcoins"));
            StoreItems.updateFromApi(pix, mc);

            if (StoreItems.isLoaded()) {
                plugin.getLogger().info("[Store] Catálogo atualizado: "
                        + StoreItems.pixKeys().size() + " Pix / "
                        + StoreItems.mobcoinsKeys().size() + " MobCoins");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Store] Falha ao atualizar catálogo: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> toMap(JsonElement el) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        if (el == null || !el.isJsonObject()) return out;
        for (var e : el.getAsJsonObject().entrySet()) {
            if (e.getValue().isJsonObject()) {
                out.put(e.getKey(), gson.fromJson(e.getValue(), Map.class));
            }
        }
        return out;
    }

    // ── Polling de compras pendentes ───────────────────────────────────────
    private void pollPendingPurchases() {
        try {
            JsonObject response = getJson(apiUrl + "/api/pending", Map.of("X-Plugin-Secret", pluginSecret));
            if (response == null) return;

            JsonArray pending = response.getAsJsonArray("pending");
            int count = pending != null ? pending.size() : 0;

            if (count > 0) {
                plugin.getLogger().info("[Store] " + count + " compra(s) pendente(s) encontrada(s)");
                for (JsonElement elem : pending) {
                    processPurchase(elem.getAsJsonObject());
                }
            }
        } catch (java.net.ConnectException e) {
            plugin.getLogger().warning("[Store] Não foi possível conectar à API: " + apiUrl);
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
            plugin.getLogger().warning("[Store] Itens Pix disponíveis: " + StoreItems.pixKeys());
            plugin.getLogger().warning("[Store] Itens MobCoins disponíveis: " + StoreItems.mobcoinsKeys());
            return;
        }

        String command = item.command().replace("%player%", playerName);
        plugin.getLogger().info("[Store] Executando comando: " + command);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                plugin.getLogger().info("[Store] ✅ Entregue " + item.name() + " para " + playerName);
            } catch (Exception e) {
                plugin.getLogger().severe("[Store] ❌ Erro ao executar comando: " + e.getMessage());
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> markDelivered(id, type));
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
            conn.getOutputStream().write(("{\"type\":\"" + type + "\"}").getBytes(StandardCharsets.UTF_8));
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Store] Erro ao marcar como entregue: " + e.getMessage());
        }
    }

    // ── Sync MobCoins balances ─────────────────────────────────────────────
    private void syncMobCoinsBalances() {
        if (mobCoinsDb == null) return;
        try {
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

            postJson(apiUrl + "/api/mobcoins/sync", "{\"balances\":" + balancesJson + "}",
                    Map.of("X-Plugin-Secret", pluginSecret));
        } catch (Exception e) {
            // Silent fail - sync will retry next cycle
        }
    }

    // ── Helpers HTTP ───────────────────────────────────────────────────────
    private JsonObject getJson(String url) throws IOException {
        return getJson(url, Map.of());
    }

    private JsonObject getJson(String url, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");
        headers.forEach(conn::setRequestProperty);

        int code = conn.getResponseCode();
        if (code != 200) {
            plugin.getLogger().warning("[Store] API retornou código " + code + " em " + url);
            conn.disconnect();
            return null;
        }
        try (var reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, JsonObject.class);
        } finally {
            conn.disconnect();
        }
    }

    private void postJson(String url, String body, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/json");
        headers.forEach(conn::setRequestProperty);
        conn.setDoOutput(true);
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        int code = conn.getResponseCode();
        if (code != 200) {
            plugin.getLogger().warning("[MobCoins] Erro ao sincronizar saldos: código " + code);
        }
        conn.disconnect();
    }
}
