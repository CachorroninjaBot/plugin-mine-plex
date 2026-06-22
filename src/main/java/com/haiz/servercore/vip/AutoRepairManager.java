package com.haiz.servercore.vip;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Auto-repair de ferramentas inspirado no UltraRepair.
 * Recursos: custo via Vault, cooldowns, comandos /repair, gratuito para VIPs.
 */
public final class AutoRepairManager implements Listener {

    private final JavaPlugin plugin;
    private final Logger log;
    private final VipStorage vipStorage;
    private final VipConfig vipConfig;

    private boolean enabled;
    private boolean applyToAll;
    private boolean useEconomy;
    private double durabilityMultiplier;
    private int cooldownHand;
    private int cooldownAll;
    private boolean playSound;
    private boolean sendMessage;
    private String messageHand;
    private String messageAll;
    private String messageCooldown;
    private String messageInsufficientFunds;
    private final Set<String> freeTiers = new HashSet<>();
    private final Set<Material> blacklistedMaterials = new HashSet<>();
    private final Map<Material, Double> materialCosts = new HashMap<>();
    private final Map<Material, Integer> customThresholds = new HashMap<>();
    private final Map<UUID, Long> cooldownsHand = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownsAll = new ConcurrentHashMap<>();

    private Object economy = null;

    public AutoRepairManager(JavaPlugin plugin, VipStorage vipStorage, VipConfig vipConfig) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.vipStorage = vipStorage;
        this.vipConfig = vipConfig;
        loadConfig();
        setupEconomy();
    }

    @SuppressWarnings("unchecked")
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        try {
            Class<?> rspClass = Class.forName("org.bukkit.plugin.RegisteredServiceProvider");
            Object rsp = Bukkit.getServicesManager().getRegistration(
                    Class.forName("net.milkbowl.vault.economy.Economy"));
            if (rsp != null) {
                java.lang.reflect.Method getProvider = rsp.getClass().getMethod("getProvider");
                economy = getProvider.invoke(rsp);
                log.info("[AutoRepair] Vault economy conectada.");
            }
        } catch (Exception e) {
            log.warning("[AutoRepair] Vault não encontrado: " + e.getMessage());
        }
    }

    private void loadConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("auto-repair");
        if (section == null) {
            enabled = false;
            return;
        }

        enabled = section.getBoolean("enabled", true);
        applyToAll = section.getBoolean("apply-to-all", false);
        useEconomy = section.getBoolean("use-economy", true);
        durabilityMultiplier = section.getDouble("durability-multiplier", 0.5);
        cooldownHand = Math.max(0, section.getInt("cooldown-hand", 5));
        cooldownAll = Math.max(0, section.getInt("cooldown-all", 10));
        playSound = section.getBoolean("play-sound", true);
        sendMessage = section.getBoolean("send-message", true);
        messageHand = section.getString("message-hand", "§8[§bHaizCore§8] §aItem na mão reparado!");
        messageAll = section.getString("message-all", "§8[§bHaizCore§8] §aTodos os itens reparados!");
        messageCooldown = section.getString("message-cooldown", "§8[§bHaizCore§8] §cAguarde §f%time% §cpara reparar novamente.");
        messageInsufficientFunds = section.getString("message-insufficient-funds", "§8[§bHaizCore§8] §cVocê precisa de §f$%cost% §cpara reparar.");

        freeTiers.clear();
        for (String tier : section.getStringList("free-tiers")) {
            freeTiers.add(tier.toLowerCase(java.util.Locale.ROOT));
        }

        blacklistedMaterials.clear();
        for (String mat : section.getStringList("blacklisted-materials")) {
            try {
                blacklistedMaterials.add(Material.valueOf(mat.toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }

        materialCosts.clear();
        ConfigurationSection costsSection = section.getConfigurationSection("material-costs");
        if (costsSection != null) {
            for (String matName : costsSection.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(matName.toUpperCase(java.util.Locale.ROOT));
                    materialCosts.put(mat, costsSection.getDouble(matName));
                } catch (IllegalArgumentException ignored) {}
            }
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

    public boolean isEnabled() { return enabled; }
    public boolean isApplyToAll() { return applyToAll; }

    public boolean isFreeTier(String tier) {
        return freeTiers.contains(tier.toLowerCase(java.util.Locale.ROOT));
    }

    public boolean hasRepairPermission(Player player) {
        return player.hasPermission("haizcore.repair") || player.hasPermission("haizcore.repair.bypass");
    }

    public boolean hasBypassPermission(Player player) {
        return player.hasPermission("haizcore.repair.bypass");
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!applyToAll) {
            VipStorage.VipSubscription sub = vipStorage.getActiveSubscription(uuid).orElse(null);
            if (sub == null) return;

            if (!isFreeTier(sub.tier()) && !hasRepairPermission(player)) return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;
        if (blacklistedMaterials.contains(item.getType())) return;

        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) return;

        int currentDurability = maxDurability - event.getDamage();
        int threshold = customThresholds.getOrDefault(item.getType(), 5);

        if (currentDurability <= threshold) {
            if (cooldownsHand.containsKey(uuid)) {
                long lastRepair = cooldownsHand.get(uuid);
                long cooldownMs = hasBypassPermission(player) ? 0 : cooldownHand * 1000L;
                if (System.currentTimeMillis() - lastRepair < cooldownMs) {
                    return;
                }
            }

            event.setCancelled(true);
            item.setDurability((short) 0);
            cooldownsHand.put(uuid, System.currentTimeMillis());

            if (playSound) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            }

            if (sendMessage) {
                player.sendMessage(messageHand);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) return;

        if (player.isSneaking()) {
            event.setCancelled(true);
            repairAll(player);
        }
    }

    public void repairHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§cVocê não está segurando nada.");
            return;
        }

        if (blacklistedMaterials.contains(item.getType())) {
            player.sendMessage("§cEste item não pode ser reparado.");
            return;
        }

        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0 || item.getDurability() == 0) {
            player.sendMessage("§cEste item não pode ser reparado.");
            return;
        }

        UUID uuid = player.getUniqueId();
        if (cooldownsHand.containsKey(uuid) && !hasBypassPermission(player)) {
            long lastRepair = cooldownsHand.get(uuid);
            long remaining = cooldownHand - (System.currentTimeMillis() - lastRepair) / 1000;
            if (remaining > 0) {
                player.sendMessage(messageCooldown.replace("%time%", remaining + "s"));
                return;
            }
        }

        double cost = calculateCost(item, player);
        if (cost > 0 && !hasBypassPermission(player)) {
            if (economy != null && useEconomy) {
                if (!hasMoney(player, cost)) {
                    player.sendMessage(messageInsufficientFunds.replace("%cost%", String.format("%.2f", cost)));
                    return;
                }
                withdrawMoney(player, cost);
            }
        }

        item.setDurability((short) 0);
        cooldownsHand.put(uuid, System.currentTimeMillis());

        if (playSound) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        }

        player.sendMessage(messageHand);
    }

    public void repairAll(Player player) {
        UUID uuid = player.getUniqueId();
        if (cooldownsAll.containsKey(uuid) && !hasBypassPermission(player)) {
            long lastRepair = cooldownsAll.get(uuid);
            long remaining = cooldownAll - (System.currentTimeMillis() - lastRepair) / 1000;
            if (remaining > 0) {
                player.sendMessage(messageCooldown.replace("%time%", remaining + "s"));
                return;
            }
        }

        int repaired = 0;
        double totalCost = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (blacklistedMaterials.contains(item.getType())) continue;

            int maxDurability = item.getType().getMaxDurability();
            if (maxDurability <= 0 || item.getDurability() == 0) continue;

            double cost = calculateCost(item, player);
            totalCost += cost;
            item.setDurability((short) 0);
            repaired++;
        }

        if (repaired == 0) {
            player.sendMessage("§cNão há itens para reparar no inventário.");
            return;
        }

        if (totalCost > 0 && !hasBypassPermission(player)) {
            if (economy != null && useEconomy) {
                if (!hasMoney(player, totalCost)) {
                    player.sendMessage(messageInsufficientFunds.replace("%cost%", String.format("%.2f", totalCost)));
                    return;
                }
                withdrawMoney(player, totalCost);
            }
        }

        cooldownsAll.put(uuid, System.currentTimeMillis());

        if (playSound) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        }

        player.sendMessage(messageAll);
    }

    private double calculateCost(ItemStack item, Player player) {
        VipStorage.VipSubscription sub = vipStorage.getActiveSubscription(player.getUniqueId()).orElse(null);
        if (sub != null && isFreeTier(sub.tier())) return 0;
        if (hasBypassPermission(player)) return 0;

        double baseCost = materialCosts.getOrDefault(item.getType(), 10.0);

        int maxDurability = item.getType().getMaxDurability();
        int durabilityRepaired = maxDurability - item.getDurability();
        return baseCost + (durabilityRepaired * durabilityMultiplier);
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        log.info("[AutoRepair] Módulo auto-repair " + (enabled ? "ativado" : "desativado") + ".");
    }

    public void unregister() {
        PlayerItemDamageEvent.getHandlerList().unregister(this);
        PlayerInteractEvent.getHandlerList().unregister(this);
        cooldownsHand.clear();
        cooldownsAll.clear();
    }

    private void log(String msg) {
        plugin.getLogger().info(msg);
    }

    @SuppressWarnings("unchecked")
    private boolean hasMoney(Player player, double amount) {
        try {
            java.lang.reflect.Method hasMethod = economy.getClass().getMethod("has", Player.class, double.class);
            return (boolean) hasMethod.invoke(economy, player, amount);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void withdrawMoney(Player player, double amount) {
        try {
            java.lang.reflect.Method withdrawMethod = economy.getClass().getMethod("withdrawPlayer", Player.class, double.class);
            withdrawMethod.invoke(economy, player, amount);
        } catch (Exception e) {
            log.warning("[AutoRepair] Falha ao debitar: " + e.getMessage());
        }
    }
}
