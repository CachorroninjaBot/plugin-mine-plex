package com.haiz.servercore.website;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catálogo de itens da loja.
 *
 * Fonte única da verdade: a minepex-api (store-items.json).
 * O StorePoller popula este cache a partir de /api/store/items no startup
 * e periodicamente, eliminando a duplicação de preços hardcodeados.
 */
public final class StoreItems {

    public record Item(String name, double price, int cost, String command) {}

    private static final Map<String, Item> PIX = new LinkedHashMap<>();
    private static final Map<String, Item> MOBCOINS = new LinkedHashMap<>();
    private static volatile boolean loaded = false;

    /** Atualiza o catálogo a partir do JSON retornado por /api/store/items. */
    public static synchronized void updateFromApi(Map<String, Map<String, Object>> pix,
                                                  Map<String, Map<String, Object>> mobcoins) {
        PIX.clear();
        MOBCOINS.clear();

        if (pix != null) {
            for (var entry : pix.entrySet()) {
                Map<String, Object> v = entry.getValue();
                PIX.put(entry.getKey(), parse(entry.getKey(), v, false));
            }
        }
        if (mobcoins != null) {
            for (var entry : mobcoins.entrySet()) {
                Map<String, Object> v = entry.getValue();
                MOBCOINS.put(entry.getKey(), parse(entry.getKey(), v, true));
            }
        }
        loaded = !PIX.isEmpty() || !MOBCOINS.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static Item parse(String id, Map<String, Object> v, boolean isMobcoins) {
        String name = (String) v.getOrDefault("name", id);
        String command = (String) v.getOrDefault("command", "");
        double price = num(v.get("price"), 0.0);
        int cost = (int) num(v.get("cost"), 0.0);
        return new Item(name, price, cost, command);
    }

    private static double num(Object o, double fallback) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) {
            try { return Double.parseDouble(s); } catch (Exception e) { return fallback; }
        }
        return fallback;
    }

    public static Item getPix(String id) { return PIX.get(id); }
    public static Item getMobCoins(String id) { return MOBCOINS.get(id); }

    public static boolean isLoaded() { return loaded; }
    public static java.util.Set<String> pixKeys() { return PIX.keySet(); }
    public static java.util.Set<String> mobcoinsKeys() { return MOBCOINS.keySet(); }
}
