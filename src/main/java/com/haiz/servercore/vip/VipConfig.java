package com.haiz.servercore.vip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class VipConfig {

    public record VipTier(
            String id,
            String displayName,
            long price,
            String emojiId,
            String emojiName,
            String description,
            List<String> perks,
            String grantCommand   // %player% será substituído
    ) {}

    private final FileConfiguration config;
    private final List<VipTier> tiers = new ArrayList<>();

    public VipConfig(FileConfiguration config) {
        this.config = config;
        load();
    }

    private void load() {
        tiers.clear();
        ConfigurationSection section = config.getConfigurationSection("vip.tiers");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection t = section.getConfigurationSection(key);
            if (t == null) continue;
            tiers.add(new VipTier(
                    key,
                    t.getString("display-name", key),
                    t.getLong("price", 0),
                    t.getString("emoji-id", ""),
                    t.getString("emoji-name", key.toLowerCase(Locale.ROOT)),
                    t.getString("description", ""),
                    t.getStringList("perks"),
                    t.getString("grant-command", "lp user %player% parent set " + key.toLowerCase(Locale.ROOT))
            ));
        }
    }

    public List<VipTier> tiers() { return Collections.unmodifiableList(tiers); }

    public Optional<VipTier> find(String id) {
        return tiers.stream().filter(t -> t.id().equalsIgnoreCase(id)).findFirst();
    }

    public String vipShopChannelId() {
        return config.getString("vip.discord.shop-channel-id", "");
    }

    public String vipLogsChannelId() {
        return config.getString("vip.discord.logs-channel-id", "");
    }

    public int linkCodeExpirySeconds() {
        return Math.max(60, config.getInt("vip.link.code-expiry-seconds", 120));
    }

    public int purchaseConfirmTimeoutSeconds() {
        return Math.max(30, config.getInt("vip.purchase.confirm-timeout-seconds", 60));
    }

    public String shopImageUrl() {
        return config.getString("vip.discord.shop-image-url",
                "https://cdn.discordapp.com/attachments/1490081242937557177/1516646686209081424/d3678e7c-7dcd-4af5-af35-fdc15eb0c811.png");
    }

    public String shopThumbnailUrl() {
        return config.getString("vip.discord.shop-thumbnail-url",
                "https://cdn.discordapp.com/icons/1375700384353484820/a66b5a142f5a32fdb1502785ca875a67.png?size=4096");
    }
}