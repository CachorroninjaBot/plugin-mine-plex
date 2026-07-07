package com.haiz.servercore.discord.events;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.config.ConfigManager;
import com.haiz.servercore.discord.webhook.WebhookUtil;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerJoinLeaveListener implements Listener {
    private final HaizServerCore plugin;
    private final Map<String, String> webhookUrlCache = new HashMap<>();
    private final Map<UUID, Long> joinTimes = new HashMap<>();

    public PlayerJoinLeaveListener(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        ConfigManager config = plugin.config();
        if (!config.isEventsEnabled()) return;
        if (!plugin.discord().isOnline()) return;

        String channelId = config.eventsChannelId();
        if (channelId == null || channelId.isBlank()) return;

        org.bukkit.entity.Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean isFirstJoin = !player.hasPlayedBefore();

        joinTimes.put(uuid, System.currentTimeMillis());

        if (isFirstJoin && config.isFirstJoinEnabled()) {
            sendEvent(channelId, player, config.eventFirstJoinFormat(), config.eventFirstJoinColor(), config.eventFirstJoinThumbnail());
        } else if (config.isEventJoinEnabled()) {
            sendEvent(channelId, player, config.eventJoinFormat(), config.eventJoinColor(), config.eventJoinThumbnail());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        ConfigManager config = plugin.config();
        if (!config.isEventsEnabled()) return;
        if (!plugin.discord().isOnline()) return;
        if (!config.isEventLeaveEnabled()) return;

        String channelId = config.eventsChannelId();
        if (channelId == null || channelId.isBlank()) return;

        org.bukkit.entity.Player player = event.getPlayer();
        joinTimes.remove(player.getUniqueId());

        sendEvent(channelId, player, config.eventLeaveFormat(), config.eventLeaveColor(), config.eventLeaveThumbnail());
    }

    private void sendEvent(String channelId, org.bukkit.entity.Player player, String format, String color, String thumbnailUrl) {
        ConfigManager config = plugin.config();

        String message = format
                .replace("%displayname%", player.getDisplayName())
                .replace("%username%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%world%", player.getWorld().getName())
                .replace("%playercount%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%playermax%", String.valueOf(Bukkit.getMaxPlayers()));

        String avatarUrl = thumbnailUrl != null ? thumbnailUrl.replace("%uuid%", player.getUniqueId().toString()) : null;

        if (config.isEventsWebhookEnabled()) {
            sendViaWebhook(channelId, player.getDisplayName(), player.getUniqueId().toString(), message, color, avatarUrl);
        } else {
            sendViaBot(channelId, message, color, avatarUrl);
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
                plugin.getLogger().warning("[Events] Falha ao enviar evento: " + e.getMessage());
            }
        });
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
