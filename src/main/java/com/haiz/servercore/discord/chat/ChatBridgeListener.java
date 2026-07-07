package com.haiz.servercore.discord.chat;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.config.ConfigManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.awt.Color;
import java.util.List;

public final class ChatBridgeListener extends ListenerAdapter {
    private final HaizServerCore plugin;

    public ChatBridgeListener(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            ConfigManager config = plugin.config();
            if (config.chatBridgeBlockBots()) return;
        }
        if (event.getAuthor().isSystem()) return;

        ConfigManager config = plugin.config();
        if (!config.isChatBridgeEnabled()) return;
        if (!config.isDiscordToMinecraftEnabled()) return;

        String channelId = event.getChannel().getId();
        List<String> allowedChannels = config.chatBridgeChannelIds();
        if (!allowedChannels.isEmpty() && !allowedChannels.contains(channelId)) return;

        String memberName = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getAuthor().getGlobalName();
        if (memberName == null || memberName.isBlank()) {
            memberName = event.getAuthor().getName();
        }

        String topRoleName = "";
        String topRoleColor = "&7";
        if (event.getMember() != null) {
            List<Role> roles = event.getMember().getRoles();
            if (!roles.isEmpty()) {
                Role topRole = roles.get(0);
                topRoleName = topRole.getName();
                Color c = topRole.getColor();
                if (c != null) {
                    topRoleColor = colorToMinecraft(c);
                }
            }
        }

        String rawMessage = event.getMessage().getContentRaw();
        if (config.chatBridgeMaxLength() > 0 && rawMessage.length() > config.chatBridgeMaxLength()) {
            rawMessage = rawMessage.substring(0, config.chatBridgeMaxLength()) + "...";
        }
        String message = rawMessage;

        String format = config.discordToMinecraftFormat();
        format = format.replace("%name%", topRoleColor + memberName + "&r");
        format = format.replace("%toprole%", topRoleName);
        format = format.replace("%toprolealias%", topRoleName);
        format = format.replace("%toprolecolor%", topRoleColor);
        format = format.replace("%message%", message);
        format = format.replace("%channelname%", event.getChannel().getName());
        format = format.replace("%userid%", event.getAuthor().getId());

        String finalMessage = colorize(format);
        String finalName = memberName;

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("haizcore.chat.receive")) {
                    player.sendMessage(finalMessage);
                }
            }
            plugin.getLogger().info("[Discord] " + finalName + ": " + message);
        });
    }

    private String colorToMinecraft(Color color) {
        if (color == null) return "&7";
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        return "§x§" + r + "§" + r + "§" + g + "§" + g + "§" + b + "§" + b;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
