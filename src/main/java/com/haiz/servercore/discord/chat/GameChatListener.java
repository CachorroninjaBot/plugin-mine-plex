package com.haiz.servercore.discord.chat;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.config.ConfigManager;
import com.haiz.servercore.discord.webhook.WebhookUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GameChatListener implements Listener {
    private final HaizServerCore plugin;
    private final Map<String, String> webhookUrlCache = new ConcurrentHashMap<>();

    public GameChatListener(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        ConfigManager config = plugin.config();
        if (!config.isChatBridgeEnabled()) return;
        if (!config.isMinecraftToDiscordEnabled()) return;
        if (!plugin.discord().isOnline()) return;

        String channelId = config.minecraftToDiscordChannelId();
        if (channelId == null || channelId.isBlank()) return;

        String prefix = config.minecraftToDiscordPrefix();
        if (prefix != null && !prefix.isBlank() && !event.getMessage().startsWith(prefix)) return;

        String message = prefix != null && !prefix.isBlank()
                ? event.getMessage().substring(prefix.length()).trim()
                : event.getMessage();

        Player player = event.getPlayer();
        String displayName = player.getDisplayName();
        String groupName = getPlayerPrimaryGroup(player);

        String format = config.minecraftToDiscordFormat();
        format = format.replace("%primarygroup%", groupName);
        format = format.replace("%displayname%", displayName);
        format = format.replace("%message%", message);
        format = format.replace("%world%", player.getWorld().getName());
        format = format.replace("%player%", player.getName());

        String finalMessage = format;
        String finalDisplayName = displayName;
        String uuid = player.getUniqueId().toString();

        if (config.isChatBridgeWebhookEnabled()) {
            sendViaWebhook(channelId, finalDisplayName, uuid, finalMessage);
        } else {
            sendViaBot(channelId, finalMessage);
        }
    }

    private void sendViaWebhook(String channelId, String displayName, String uuid, String message) {
        ConfigManager config = plugin.config();
        String webhookUrl = getWebhookUrl(channelId);
        if (webhookUrl == null) return;

        String avatarUrl = config.chatBridgeWebhookAvatar().replace("%uuid%", uuid);
        String webhookName = config.chatBridgeWebhookName().replace("%displayname%", displayName);

        WebhookUtil.send(webhookUrl, webhookName, avatarUrl, message);
    }

    private void sendViaBot(String channelId, String message) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Guild guild = plugin.discord().guild();
                if (guild == null) return;
                TextChannel channel = guild.getTextChannelById(channelId);
                if (channel == null) return;
                channel.sendMessage(message).queue();
            } catch (Exception e) {
                plugin.getLogger().warning("[ChatBridge] Falha ao enviar mensagem: " + e.getMessage());
            }
        });
    }

    private String getWebhookUrl(String channelId) {
        return webhookUrlCache.computeIfAbsent(channelId, id -> {
            try {
                Guild guild = plugin.discord().guild();
                if (guild == null) return null;
                TextChannel channel = guild.getTextChannelById(id);
                if (channel == null) return null;

                return channel.retrieveWebhooks().complete().stream()
                        .filter(wh -> wh.getName().equals("HaizServerCore"))
                        .findFirst()
                        .map(wh -> wh.getUrl())
                        .orElseGet(() -> {
                            var webhook = channel.createWebhook("HaizServerCore").complete();
                            return webhook.getUrl();
                        });
            } catch (Exception e) {
                return null;
            }
        });
    }

    private String getPlayerPrimaryGroup(Player player) {
        try {
            net.milkbowl.vault.chat.Chat chat = org.bukkit.Bukkit.getServicesManager()
                    .getRegistration(net.milkbowl.vault.chat.Chat.class) != null
                    ? org.bukkit.Bukkit.getServicesManager()
                    .getRegistration(net.milkbowl.vault.chat.Chat.class).getProvider()
                    : null;
            if (chat != null) {
                return chat.getPrimaryGroup(null, player);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[ChatBridge] Falha ao obter grupo Vault: " + e.getMessage());
        }
        return "player";
    }
}
