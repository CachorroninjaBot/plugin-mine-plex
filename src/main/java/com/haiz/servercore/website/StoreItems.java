package com.haiz.servercore.website;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StoreItems {

    public record Item(String name, double price, String command) {}

    public static final Map<String, Item> PIX = new LinkedHashMap<>();
    public static final Map<String, Item> MOBCOINS = new LinkedHashMap<>();

    static {
        PIX.put("vip",    new Item("Rank VIP",    15.00,  "lp user %player% parent addtemp vip 30d"));
        PIX.put("elite",  new Item("Rank ELITE",  35.00,  "lp user %player% parent addtemp elite 30d"));
        PIX.put("ultra",  new Item("Rank ULTRA",  60.00,  "lp user %player% parent addtemp ultra 30d"));
        PIX.put("media",  new Item("Rank MÍDIA",  100.00, "lp user %player% parent addtemp media 30d"));
        PIX.put("famous", new Item("Rank FAMOSO", 150.00, "lp user %player% parent addtemp famous 30d"));

        MOBCOINS.put("nether_star",     new Item("Estrela do Nether",      50000,  "give %player% nether_star 1"));
        MOBCOINS.put("heavy_core",      new Item("Núcleo Pesado",          100000, "give %player% heavy_core 1"));
        MOBCOINS.put("totem",           new Item("Totem da Imortalidade",  50000,  "give %player% totem_of_undying 1"));
        MOBCOINS.put("elytra",          new Item("Elytra",                 5000,   "give %player% elytra 1"));
        MOBCOINS.put("trident",         new Item("Tridente",               10000,  "give %player% trident 1"));
        MOBCOINS.put("enchanted_apple", new Item("Maçã Dourada Encantada", 40000,  "give %player% enchanted_golden_apple 1"));
        MOBCOINS.put("lucky_card",      new Item("Carta da Sorte",         600,    "givetokens %player% 1 lucky"));
        MOBCOINS.put("super_stick",     new Item("Bastão Super (64)",      12000,  "mm i give %player% super_stick 64"));
        MOBCOINS.put("flare",           new Item("Sinalizador de Comboio", 8000,   "envoy flare default %player% 1"));
        MOBCOINS.put("vip_30d",         new Item("VIP (30 dias)",          50000,  "lp user %player% parent addtemp vip 30d"));
        MOBCOINS.put("elite_30d",       new Item("ELITE (30 dias)",        100000, "lp user %player% parent addtemp elite 30d"));
        MOBCOINS.put("ultra_30d",       new Item("ULTRA (30 dias)",        150000, "lp user %player% parent addtemp ultra 30d"));
        MOBCOINS.put("media_30d",       new Item("MÍDIA (30 dias)",        200000, "lp user %player% parent addtemp media 30d"));
        MOBCOINS.put("famous_30d",      new Item("FAMOSO (30 dias)",       250000, "lp user %player% parent addtemp famous 30d"));
    }

    public static Item getPix(String id) { return PIX.get(id); }
    public static Item getMobCoins(String id) { return MOBCOINS.get(id); }
}
