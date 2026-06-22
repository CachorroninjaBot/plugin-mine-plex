package com.haiz.servercore.vip;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobCoinsHook {

    private final JavaPlugin plugin;
    private final Logger log;
    private Object api;
    private boolean apiAvailable;
    private boolean commandFallback;

    public MobCoinsHook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        init();
    }

    private void init() {
        Plugin mc = Bukkit.getPluginManager().getPlugin("IridiumMobCoins");
        if (mc == null) {
            mc = Bukkit.getPluginManager().getPlugin("MobCoins");
        }
        if (mc == null) {
            log.warning("[VipShop] Plugin MobCoins não encontrado. Compras desativadas.");
            return;
        }

        try {
            Class<?> apiClass = Class.forName("com.iridium.iridiummobcoins.IridiumMobCoins");
            Method getInstance = apiClass.getMethod("getInstance");
            this.api = getInstance.invoke(null);
            this.apiAvailable = true;
            log.info("[VipShop] IridiumMobCoins API conectada.");
        } catch (Exception e) {
            try {
                Class<?> apiClass = Class.forName("me.gypopo.mobcoins.api.MobCoinsAPI");
                Method getInstance = apiClass.getMethod("getInstance");
                this.api = getInstance.invoke(null);
                this.apiAvailable = true;
                log.info("[VipShop] MobCoins API conectada.");
            } catch (Exception e2) {
                log.warning("[VipShop] Falha ao conectar API MobCoins: " + e2.getMessage()
                        + ". Usando fallback por comando.");
                this.apiAvailable = false;
                this.commandFallback = true;
            }
        }
    }

    public double getBalance(UUID uuid) {
        if (!isAvailable()) return -1;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        if (apiAvailable && api != null) {
            try {
                Class<?> apiClass = api.getClass();

                if (apiClass.getName().contains("iridium")) {
                    Method getDb = apiClass.getMethod("getDatabaseManager");
                    Object dbManager = getDb.invoke(api);
                    Method getUser = dbManager.getClass().getMethod("getUser", UUID.class);
                    Object user = getUser.invoke(dbManager, uuid);
                    if (user != null) {
                        Method getMobcoins = user.getClass().getMethod("getMobcoins");
                        Object result = getMobcoins.invoke(user);
                        if (result instanceof Number n) {
                            double balance = n.doubleValue();
                            log.info("[VipShop] Saldo de " + player.getName() + ": " + balance + " MC");
                            return balance;
                        }
                    } else {
                        log.info("[VipShop] Jogador " + player.getName() + " não encontrado no IridiumMobCoins. Retornando 0.");
                        return 0;
                    }
                } else {
                    Method m = apiClass.getMethod("getMobCoins", OfflinePlayer.class);
                    Object result = m.invoke(api, player);
                    if (result instanceof Number n) return n.doubleValue();
                }
            } catch (Exception e) {
                log.warning("[VipShop] getBalance falhou para " + player.getName() + ": " + e.getMessage());
            }
        }
        return -1;
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (!isAvailable()) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        if (apiAvailable && api != null) {
            try {
                Class<?> apiClass = api.getClass();

                if (apiClass.getName().contains("iridium")) {
                    Method getDb = apiClass.getMethod("getDatabaseManager");
                    Object dbManager = getDb.invoke(api);
                    Method getUser = dbManager.getClass().getMethod("getUser", UUID.class);
                    Object user = getUser.invoke(dbManager, uuid);
                    if (user != null) {
                        Method getMobcoins = user.getClass().getMethod("getMobcoins");
                        int current = (int) getMobcoins.invoke(user);
                        int newBalance = current - (int) amount;
                        if (newBalance < 0) return false;
                        Method setMobcoins = user.getClass().getMethod("setMobcoins", int.class);
                        setMobcoins.invoke(user, newBalance);
                        return true;
                    }
                } else {
                    Method take = apiClass.getMethod("takeMobCoins", OfflinePlayer.class, double.class);
                    take.invoke(api, player, amount);
                    return true;
                }
            } catch (Exception e) {
                log.warning("[VipShop] withdraw via API falhou: " + e.getMessage() + ". Tentando comando.");
            }
        }

        if (commandFallback) {
            String cmd = "mobcoins remove " + player.getName() + " " + (int) amount;
            Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            );
            return true;
        }
        return false;
    }

    public boolean deposit(UUID uuid, double amount) {
        if (!isAvailable()) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        if (apiAvailable && api != null) {
            try {
                Class<?> apiClass = api.getClass();

                if (apiClass.getName().contains("iridium")) {
                    Method getDb = apiClass.getMethod("getDatabaseManager");
                    Object dbManager = getDb.invoke(api);
                    Method getUser = dbManager.getClass().getMethod("getUser", UUID.class);
                    Object user = getUser.invoke(dbManager, uuid);
                    if (user != null) {
                        Method getMobcoins = user.getClass().getMethod("getMobcoins");
                        int current = (int) getMobcoins.invoke(user);
                        int newBalance = current + (int) amount;
                        Method setMobcoins = user.getClass().getMethod("setMobcoins", int.class);
                        setMobcoins.invoke(user, newBalance);
                        return true;
                    }
                } else {
                    Method give = apiClass.getMethod("giveMobCoins", OfflinePlayer.class, double.class);
                    give.invoke(api, player, amount);
                    return true;
                }
            } catch (Exception e) {
                log.warning("[VipShop] deposit via API falhou: " + e.getMessage() + ". Tentando comando.");
            }
        }

        if (commandFallback) {
            String cmd = "mobcoins add " + player.getName() + " " + (int) amount;
            Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            );
            return true;
        }
        return false;
    }

    public boolean isAvailable() { return apiAvailable || commandFallback; }
}
