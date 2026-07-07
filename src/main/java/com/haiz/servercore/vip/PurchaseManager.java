package com.haiz.servercore.vip;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Gerencia o fluxo de compra após a vinculação estar confirmada.
 *
 * Fluxo:
 *  1. Usuário seleciona VIP (dropdown) → PurchaseManager.startPurchase()
 *  2. Bot mostra embed com detalhes + botão Confirmar / Cancelar
 *  3. Usuário clica Confirmar → PurchaseManager.executePurchase()
 *  4. Plugin verifica tier atual, remove se necessário, debita MobCoins e executa grant-command
 *
 * VIPs são mutuamente exclusivos — só pode ter UM plano VIP por vez.
 */
public final class PurchaseManager {

    public record PendingPurchase(String discordId, UUID uuid, String mcName,
                                   VipConfig.VipTier tier, long expiresAt) {}

    private record VipCacheEntry(String tier, long expiresAt) {}

    private final JavaPlugin plugin;
    private final MobCoinsHook mobCoins;
    private final LinkStorage linkStorage;
    private final VipConfig vipConfig;
    private final VipStorage vipStorage;
    private final Map<String, PendingPurchase> pending = new ConcurrentHashMap<>();
    private final Map<UUID, VipCacheEntry> vipCache = new ConcurrentHashMap<>();

    private static final List<String> VIP_GROUP_IDS = VipConfig.VIP_GROUP_IDS;
    private static final long CACHE_TTL_SECONDS = 300;

    public PurchaseManager(JavaPlugin plugin, MobCoinsHook mobCoins,
                           LinkStorage linkStorage, VipConfig vipConfig, VipStorage vipStorage) {
        this.plugin = plugin;
        this.mobCoins = mobCoins;
        this.linkStorage = linkStorage;
        this.vipConfig = vipConfig;
        this.vipStorage = vipStorage;
    }

    /**
     * Registra a intenção de compra.
     * @return false se já há uma compra pendente não expirada
     */
    public boolean startPurchase(String discordId, UUID uuid, String mcName, VipConfig.VipTier tier) {
        PendingPurchase existing = pending.get(discordId);
        if (existing != null && existing.expiresAt() >= now()) {
            return false;
        }
        long expires = now() + vipConfig.purchaseConfirmTimeoutSeconds();
        pending.put(discordId, new PendingPurchase(discordId, uuid, mcName, tier, expires));
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,
                () -> pending.remove(discordId), 20L * vipConfig.purchaseConfirmTimeoutSeconds());
        return true;
    }

    public enum PurchaseResult {
        SUCCESS,
        SUCCESS_UPGRADE,
        NO_PENDING,
        EXPIRED,
        INSUFFICIENT_FUNDS,
        MOBCOINS_UNAVAILABLE,
        ALREADY_HAS_VIP,
        SAME_TIER
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

        Optional<String> currentTier = getCurrentVipTier(p.uuid());
        if (currentTier.isPresent()) {
            String current = currentTier.get();
            String target = p.tier().id().toLowerCase(java.util.Locale.ROOT);
            if (current.equals(target)) {
                return PurchaseResult.SAME_TIER;
            }
        }

        double balance = mobCoins.getBalance(p.uuid());
        if (balance < 0) return PurchaseResult.MOBCOINS_UNAVAILABLE;
        if (balance < p.tier().price()) return PurchaseResult.INSUFFICIENT_FUNDS;

        boolean ok = mobCoins.withdraw(p.uuid(), p.tier().price());
        if (!ok) return PurchaseResult.MOBCOINS_UNAVAILABLE;

        String playerName = resolvePlayerName(p.uuid());
        if (playerName == null) {
            plugin.getLogger().warning("[VipShop] Não foi possível resolver o nome do jogador: " + p.uuid() + ". Estornando coins.");
            boolean refunded = mobCoins.deposit(p.uuid(), p.tier().price());
            if (!refunded) {
                plugin.getLogger().severe("[VipShop] FALHA AO ESTORNAR " + p.tier().price() + " coins para " + p.uuid() + ". Contato manual necessário.");
            }
            return PurchaseResult.MOBCOINS_UNAVAILABLE;
        }

        if (currentTier.isPresent()) {
            String oldTier = currentTier.get();
            revokeVipGroup(playerName, oldTier);
            plugin.getLogger().info("[VipShop] Tier anterior removido: " + oldTier + " → " + p.tier().id() + " para " + playerName);
        }

        String cmd = p.tier().grantCommand().replace("%player%", playerName);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));

        long now = System.currentTimeMillis() / 1000L;
        long expiresAt = now + VipStorage.durationSeconds();
        vipStorage.saveSubscription(p.uuid(), p.tier().id(), now, expiresAt);

        linkStorage.logPurchase(p.discordId(), p.uuid(), p.mcName(), p.tier().id(), p.tier().price());

        invalidateVipCache(p.uuid());

        if (currentTier.isPresent()) {
            return PurchaseResult.SUCCESS_UPGRADE;
        }
        return PurchaseResult.SUCCESS;
    }

    /**
     * Verifica o tier VIP atual do jogador usando cache em memória.
     */
    public Optional<String> getCurrentVipTier(UUID uuid) {
        long now = now();
        VipCacheEntry cached = vipCache.get(uuid);
        if (cached != null && cached.expiresAt() > now) {
            return Optional.of(cached.tier());
        }

        Optional<String> tier = vipStorage.getActiveSubscription(uuid).map(VipStorage.VipSubscription::tier);
        if (tier.isPresent()) {
            vipCache.put(uuid, new VipCacheEntry(tier.get(), now + CACHE_TTL_SECONDS));
        } else {
            vipCache.remove(uuid);
        }
        return tier;
    }

    /**
     * Invalida o cache de VIP para um jogador específico.
     */
    public void invalidateVipCache(UUID uuid) {
        vipCache.remove(uuid);
    }

    /**
     * Remove todos os grupos VIP do jogador via LuckPerms (parent remove).
     */
    public void revokeAllVipGroups(UUID uuid) {
        String playerName = resolvePlayerName(uuid);
        if (playerName == null) return;
        for (String groupId : VIP_GROUP_IDS) {
            revokeVipGroup(playerName, groupId);
        }
    }

    private void revokeVipGroup(String playerName, String groupId) {
        String revokeCmd = "lp user " + playerName + " parent remove " + groupId;
        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), revokeCmd));
    }

    private String resolvePlayerName(UUID uuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName();
        if (name != null) return name;
        Player online = Bukkit.getPlayer(uuid);
        return online != null ? online.getName() : null;
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

    private long now() { return com.haiz.servercore.utils.TimeUtils.nowSeconds(); }
}
