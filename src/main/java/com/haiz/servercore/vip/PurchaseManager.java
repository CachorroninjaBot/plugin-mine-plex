package com.haiz.servercore.vip;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Gerencia o fluxo de compra após a vinculação estar confirmada.
 *
 * Fluxo:
 *  1. Usuário seleciona VIP (dropdown) → PurchaseManager.startPurchase()
 *  2. Bot mostra embed com detalhes + botão Confirmar / Cancelar
 *  3. Usuário clica Confirmar → PurchaseManager.executePurchase()
 *  4. Plugin debita MobCoins e executa o grant-command no console
 */
public final class PurchaseManager {

    public record PendingPurchase(String discordId, UUID uuid, String mcName,
                                   VipConfig.VipTier tier, long expiresAt) {}

    private final JavaPlugin plugin;
    private final MobCoinsHook mobCoins;
    private final LinkStorage linkStorage;
    private final VipConfig vipConfig;
    private final Map<String, PendingPurchase> pending = new ConcurrentHashMap<>();

    public PurchaseManager(JavaPlugin plugin, MobCoinsHook mobCoins,
                           LinkStorage linkStorage, VipConfig vipConfig) {
        this.plugin = plugin;
        this.mobCoins = mobCoins;
        this.linkStorage = linkStorage;
        this.vipConfig = vipConfig;
    }

    /** Registra a intenção de compra. Retorna false se já há uma compra pendente. */
    public boolean startPurchase(String discordId, UUID uuid, String mcName, VipConfig.VipTier tier) {
        long expires = now() + vipConfig.purchaseConfirmTimeoutSeconds();
        pending.put(discordId, new PendingPurchase(discordId, uuid, mcName, tier, expires));
        // Auto-expirar
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,
                () -> pending.remove(discordId), 20L * vipConfig.purchaseConfirmTimeoutSeconds());
        return true;
    }

    public enum PurchaseResult {
        SUCCESS,
        NO_PENDING,
        EXPIRED,
        INSUFFICIENT_FUNDS,
        MOBCOINS_UNAVAILABLE
    }

    /**
     * Chamado quando o usuário clica em Confirmar no Discord.
     * Deve ser chamado de thread assíncrona (já vem do JDA).
     */
    public PurchaseResult executePurchase(String discordId) {
        PendingPurchase p = pending.remove(discordId);
        if (p == null) return PurchaseResult.NO_PENDING;
        if (p.expiresAt() < now()) return PurchaseResult.EXPIRED;
        if (!mobCoins.isAvailable()) return PurchaseResult.MOBCOINS_UNAVAILABLE;

        double balance = mobCoins.getBalance(p.uuid());
        // balance == -1 significa que não conseguimos consultar; prosseguimos (o withdraw falhará se saldo insuficiente)
        if (balance >= 0 && balance < p.tier().price()) return PurchaseResult.INSUFFICIENT_FUNDS;

        boolean ok = mobCoins.withdraw(p.uuid(), p.tier().price());
        if (!ok) return PurchaseResult.MOBCOINS_UNAVAILABLE;

        // Executar grant-command no thread principal do Bukkit
        String cmd = p.tier().grantCommand().replace("%player%", p.mcName());
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));

        linkStorage.logPurchase(p.discordId(), p.uuid(), p.mcName(), p.tier().id(), p.tier().price());
        return PurchaseResult.SUCCESS;
    }

    public void cancelPurchase(String discordId) {
        pending.remove(discordId);
    }

    public PendingPurchase getPending(String discordId) {
        PendingPurchase p = pending.get(discordId);
        if (p == null || p.expiresAt() < now()) {
            pending.remove(discordId);
            return null;
        }
        return p;
    }

    private long now() { return System.currentTimeMillis() / 1000L; }
}