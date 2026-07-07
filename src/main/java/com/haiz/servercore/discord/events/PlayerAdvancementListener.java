package com.haiz.servercore.discord.events;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.config.ConfigManager;
import com.haiz.servercore.discord.webhook.WebhookUtil;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.HashMap;
import java.util.Map;

public final class PlayerAdvancementListener implements Listener {
    private final HaizServerCore plugin;
    private final Map<String, String> webhookUrlCache = new HashMap<>();

    public PlayerAdvancementListener(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        ConfigManager config = plugin.config();
        if (!config.isEventsEnabled()) return;
        if (!config.isEventAdvancementEnabled()) return;
        if (!plugin.discord().isOnline()) return;

        String channelId = config.eventsChannelId();
        if (channelId == null || channelId.isBlank()) return;

        org.bukkit.entity.Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();

        String advancementName = advancement.getKey().getKey();
        String[] parts = advancementName.split("/");
        String displayName = parts[parts.length - 1].replace("-", " ").replace("_", " ");
        displayName = capitalizeWords(displayName);

        String message = config.eventAdvancementFormat()
                .replace("%displayname%", player.getDisplayName())
                .replace("%username%", player.getName())
                .replace("%advancement%", displayName)
                .replace("%world%", player.getWorld().getName());

        String avatarUrl = config.eventAdvancementThumbnail().replace("%uuid%", player.getUniqueId().toString());

        if (config.isEventsWebhookEnabled()) {
            sendViaWebhook(channelId, player.getDisplayName(), player.getUniqueId().toString(), message, config.eventAdvancementColor(), avatarUrl);
        } else {
            sendViaBot(channelId, message, config.eventAdvancementColor(), avatarUrl);
        }
    }

    private void sendViaWebhook(String channelId, String displayName, String uuid, String message, String color, String avatarUrl) {
        String webhookUrl = getWebhookUrl(channelId);
        if (webhookUrl == null) return;

        JsonObject embed = WebhookUtil.buildEmbed(null, message, color, avatarUrl, null);
        WebhookUtil.send(webhookUrl, displayName, avatarUrl, "", embed);
    }

    private void sendViaBot(String channelId, String message, String color, String avatarUrl) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Guild guild = plugin.discord().jda().getGuilds().stream().findFirst().orElse(null);
                if (guild == null) return;
                TextChannel channel = guild.getTextChannelById(channelId);
                if (channel == null) return;

                JsonObject embed = WebhookUtil.buildEmbed(null, message, color, avatarUrl, null);

                net.dv8tion.jda.api.entities.MessageEmbed jdaEmbed = net.dv8tion.jda.api.EmbedBuilder.fromData(
                        net.dv8tion.jda.api.utils.data.DataObject.fromJson(embed.toString())
                ).build();

                channel.sendMessageEmbeds(jdaEmbed).queue();
            } catch (Exception e) {
                plugin.getLogger().warning("[Events] Falha ao enviar evento de avanço: " + e.getMessage());
            }
        });
    }

    private String capitalizeWords(String input) {
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    private String getWebhookUrl(String channelId) {
        return webhookUrlCache.computeIfAbsent(channelId, id -> {
            try {
                Guild guild = plugin.discord().jda().getGuilds().stream().findFirst().orElse(null);
                if (guild == null) return null;
                TextChannel channel = guild.getTextChannelById(id);
                if (channel == null) return null;

                return channel.retrieveWebhooks().complete().stream()
                        .filter(wh -> wh.getName().equals("HaizServerCore-Events"))
                        .findFirst()
                        .map(wh -> wh.getUrl())
                        .orElseGet(() -> {
                            var webhook = channel.createWebhook("HaizServerCore-Events").complete();
                            return webhook.getUrl();
                        });
            } catch (Exception e) {
                return null;
            }
        });
    }
}
