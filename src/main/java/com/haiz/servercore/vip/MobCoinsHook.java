package com.haiz.servercore.vip;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hook para o plugin MobCoins via reflexão.
 *
 * Compatível com a maioria das versões do MobCoins (EpicSpawners / SaberFactions / standalone).
 * A API real varia conforme o fork; ajuste os nomes dos métodos se necessário.
 *
 * Se preferir integração via comando (/mobcoins take <nick> <valor>),
 * comente o bloco "API path" e descomente o bloco "Command fallback".
 */
public final class MobCoinsHook {

    private final Logger log;
    private Object api; // instância da MobCoinsAPI
    private boolean available;

    public MobCoinsHook(JavaPlugin plugin) {
        this.log = plugin.getLogger();
        init();
    }

    private void init() {
        Plugin mc = Bukkit.getPluginManager().getPlugin("MobCoins");
        if (mc == null) {
            log.warning("[VipShop] Plugin MobCoins não encontrado. Compras desativadas.");
            return;
        }
        try {
            // ── API path (ajuste o pacote/classe conforme seu fork) ──────────────
            Class<?> apiClass = Class.forName("me.gypopo.mobcoins.api.MobCoinsAPI");
            Method getInstance = apiClass.getMethod("getInstance");
            this.api = getInstance.invoke(null);
            this.available = true;
            log.info("[VipShop] MobCoins API conectada.");
        } catch (Exception e) {
            log.warning("[VipShop] Falha ao conectar MobCoinsAPI: " + e.getMessage()
                    + ". Usando fallback por comando.");
            this.available = true; // fallback por comando ainda funciona
        }
    }

    /** Retorna o saldo de MobCoins do jogador (0 se indisponível). */
    public double getBalance(UUID uuid) {
        if (!available) return 0;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (api != null) {
            try {
                Method m = api.getClass().getMethod("getMobCoins", OfflinePlayer.class);
                Object result = m.invoke(api, player);
                if (result instanceof Number n) return n.doubleValue();
            } catch (Exception e) {
                log.warning("[VipShop] getBalance falhou: " + e.getMessage());
            }
        }
        // Fallback: não consegue consultar via API, retorna -1 para indicar "desconhecido"
        return -1;
    }

    /**
     * Debita o valor do jogador.
     * @return true se debitado com sucesso
     */
    public boolean withdraw(UUID uuid, double amount) {
        if (!available) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        // ── API path ─────────────────────────────────────────────────────────
        if (api != null) {
            try {
                Method take = api.getClass().getMethod("takeMobCoins", OfflinePlayer.class, double.class);
                take.invoke(api, player, amount);
                return true;
            } catch (Exception e) {
                log.warning("[VipShop] withdraw via API falhou: " + e.getMessage() + ". Tentando comando.");
            }
        }

        // ── Command fallback ─────────────────────────────────────────────────
        // Substitua "mobcoins take" pelo comando correto do seu plugin
        String cmd = "mobcoins take " + player.getName() + " " + (long) amount;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        return true;
    }

    public boolean isAvailable() { return available; }
}