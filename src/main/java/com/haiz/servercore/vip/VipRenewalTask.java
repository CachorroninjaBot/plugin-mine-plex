package com.haiz.servercore.vip;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public final class VipRenewalTask {

    private static final long CHECK_INTERVAL_TICKS = 20L * 60 * 60;
    private static final long EXPIRY_WARNING_SECONDS = 24L * 60 * 60;
    private static final List<String> VIP_GROUP_IDS = List.of("vip", "elite", "ultra", "midia", "famoso");

    private final JavaPlugin plugin;
    private final Logger log;
    private final VipStorage vipStorage;
    private final MobCoinsHook mobCoins;
    private final VipConfig vipConfig;
    private final java.util.function.Consumer<UUID> cacheInvalidator;
    private int taskId = -1;

    public VipRenewalTask(JavaPlugin plugin, VipStorage vipStorage, MobCoinsHook mobCoins, VipConfig vipConfig, java.util.function.Consumer<UUID> cacheInvalidator) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.vipStorage = vipStorage;
        this.mobCoins = mobCoins;
        this.vipConfig = vipConfig;
        this.cacheInvalidator = cacheInvalidator;
    }

    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkRenewals, 20L * 60, CHECK_INTERVAL_TICKS).getTaskId();
        log.info("[VipShop] Tarefa de renovação automática iniciada.");
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void checkRenewals() {
        long now = System.currentTimeMillis() / 1000L;
        long warnBefore = now + EXPIRY_WARNING_SECONDS;

        var expiring = vipStorage.getVipsExpiringBefore(warnBefore);

        for (VipStorage.ExpiringVip vip : expiring) {
            long remaining = vip.expiresAt() - now;

            if (remaining <= 0) {
                handleExpired(vip.uuid(), vip.tier());
            } else if (vipStorage.getAutoRenew(vip.uuid())) {
                handleAutoRenew(vip.uuid(), vip.tier(), vip.expiresAt());
            } else {
                notifyExpiring(vip.uuid(), remaining);
            }
        }
    }

    private void handleExpired(UUID uuid, String tier) {
        boolean autoRenew = vipStorage.getAutoRenew(uuid);

        if (autoRenew && mobCoins.isAvailable()) {
            if (tryRenew(uuid, tier)) return;
        }

        revokeVip(uuid, tier);
        vipStorage.removeSubscription(uuid);
        cacheInvalidator.accept(uuid);
        notifyPlayer(uuid, "§cSeu VIP §f" + tier + " §cexpirou e foi removido.");

        log.info("[VipShop] VIP expirado: " + uuid + " (tier: " + tier + ")");
    }

    private void handleAutoRenew(UUID uuid, String tier, long currentExpiresAt) {
        if (!mobCoins.isAvailable()) return;

        long renewAt = currentExpiresAt - EXPIRY_WARNING_SECONDS;
        long now = System.currentTimeMillis() / 1000L;

        if (now >= renewAt) {
            tryRenew(uuid, tier);
        }
    }

    private boolean tryRenew(UUID uuid, String tier) {
        var tierOpt = vipConfig.find(tier);
        if (tierOpt.isEmpty()) {
            log.warning("[VipShop] Tier '" + tier + "' não encontrado no config para renovação.");
            return false;
        }

        VipConfig.VipTier vipTier = tierOpt.get();
        double balance = mobCoins.getBalance(uuid);

        if (balance >= 0 && balance < vipTier.price()) {
            notifyPlayer(uuid, "§eSeu VIP §f" + tier + " §eeá expirando, mas você não tem MobCoins suficientes para renovação automática (" + (int) vipTier.price() + " needed).");
            log.info("[VipShop] Renovação automática falhou (saldo insuficiente): " + uuid);
            return false;
        }

        boolean ok = mobCoins.withdraw(uuid, vipTier.price());
        if (!ok) {
            notifyPlayer(uuid, "§cFalha ao renovar VIP §f" + tier + "§c. Saldo insuficiente.");
            return false;
        }

        long now = System.currentTimeMillis() / 1000L;
        long newExpiry = now + VipStorage.durationSeconds();
        vipStorage.renewSubscription(uuid, newExpiry);

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String grantCmd = vipTier.grantCommand().replace("%player%", player.getName());
        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), grantCmd));

        notifyPlayer(uuid, "§aSeu VIP §f" + tier + " §afoi renovado automaticamente! §7Expira em 30 dias.");
        log.info("[VipShop] VIP renovado automaticamente: " + uuid + " (tier: " + tier + ")");
        return true;
    }

    private void revokeVip(UUID uuid, String tier) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String playerName = player.getName();
        if (playerName == null) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) playerName = online.getName();
        }
        if (playerName == null) return;

        for (String groupId : VIP_GROUP_IDS) {
            String revokeCmd = "lp user " + playerName + " parent remove " + groupId;
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), revokeCmd));
        }
    }

    private void notifyExpiring(UUID uuid, long remainingSeconds) {
        long hours = remainingSeconds / 3600;
        notifyPlayer(uuid, "§eSeu VIP §fexpira em §c" + hours + " horas§e. Use §f/vipconfig autoresnovar §epara ativar renovação automática.");
    }

    private void notifyPlayer(UUID uuid, String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage("§8[§bHaizCore§8] " + message));
        }
    }
}
