package com.haiz.servercore.vip;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Auto-repair de ferramentas para VIPs.
 * Repara automaticamente ferramentas quando a durabilidade atinge o threshold.
 * Suporta cooldown, configuração por material, efeitos sonoros e mensagens personalizadas.
 */
public final class AutoRepairManager implements Listener {

    private final JavaPlugin plugin;
    private final VipStorage vipStorage;
    private final VipConfig vipConfig;
    private boolean enabled;
    private boolean applyToAll;
    private int durabilityThreshold;
    private int cooldownSeconds;
    private boolean playSound;
    private boolean sendMessage;
    private String messageFormat;
    private final Set<String> eligibleTiers = new HashSet<>();
    private final Set<Material> blacklistedMaterials = new HashSet<>();
    private final Map<Material, Integer> customThresholds = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public AutoRepairManager(JavaPlugin plugin, VipStorage vipStorage, VipConfig vipConfig) {
        this.plugin = plugin;
        this.vipStorage = vipStorage;
        this.vipConfig = vipConfig;
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("auto-repair");
        if (section == null) {
            enabled = false;
            return;
        }

        enabled = section.getBoolean("enabled", true);
        applyToAll = section.getBoolean("apply-to-all", false);
        durabilityThreshold = Math.max(1, section.getInt("durability-threshold", 5));
        cooldownSeconds = Math.max(0, section.getInt("cooldown-seconds", 10));
        playSound = section.getBoolean("play-sound", true);
        sendMessage = section.getBoolean("send-message", true);
        messageFormat = section.getString("message", "§8[§bHaizCore§8] §a§l%item% §areparado automaticamente!");

        eligibleTiers.clear();
        for (String tier : section.getStringList("tiers")) {
            eligibleTiers.add(tier.toLowerCase(java.util.Locale.ROOT));
        }

        blacklistedMaterials.clear();
        for (String mat : section.getStringList("blacklisted-materials")) {
            try {
                blacklistedMaterials.add(Material.valueOf(mat.toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }

        customThresholds.clear();
        ConfigurationSection customSection = section.getConfigurationSection("custom-thresholds");
        if (customSection != null) {
            for (String matName : customSection.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(matName.toUpperCase(java.util.Locale.ROOT));
                    customThresholds.put(mat, Math.max(1, customSection.getInt(matName)));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void reload() {
        loadConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEligibleTier(String tier) {
        return eligibleTiers.contains(tier.toLowerCase(java.util.Locale.ROOT));
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!applyToAll) {
            VipStorage.VipSubscription sub = vipStorage.getActiveSubscription(uuid).orElse(null);
            if (sub == null) return;

            if (!isEligibleTier(sub.tier())) return;

            if (!vipStorage.getAutoRepair(uuid)) return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;

        if (blacklistedMaterials.contains(item.getType())) return;

        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) return;

        int currentDurability = maxDurability - event.getDamage();
        int threshold = customThresholds.getOrDefault(item.getType(), durabilityThreshold);

        if (currentDurability <= threshold) {
            if (cooldowns.containsKey(uuid)) {
                long lastRepair = cooldowns.get(uuid);
                if (System.currentTimeMillis() - lastRepair < cooldownSeconds * 1000L) {
                    return;
                }
            }

            event.setCancelled(true);
            item.setDurability((short) 0);
            cooldowns.put(uuid, System.currentTimeMillis());

            if (playSound) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            }

            if (sendMessage) {
                String msg = messageFormat.replace("%item%", item.getType().name().replace("_", " "));
                player.sendMessage(msg);
            }
        }
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[AutoRepair] Módulo auto-repair " + (enabled ? "ativado" : "desativado") + ".");
    }

    public void unregister() {
        PlayerItemDamageEvent.getHandlerList().unregister(this);
        cooldowns.clear();
    }
}
