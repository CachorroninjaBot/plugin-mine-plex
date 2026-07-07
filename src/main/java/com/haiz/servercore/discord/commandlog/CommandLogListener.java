package com.haiz.servercore.discord.commandlog;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.gson.JsonObject;
import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.discord.webhook.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CommandLogListener {
    private final HaizServerCore plugin;
    private final CommandLogConfig config;
    private ProtocolManager protocolManager;
    private PacketAdapter packetListener;

    // Cache para vinculação Discord
    private final Map<UUID, String> discordLinks = new HashMap<>();

    public CommandLogListener(HaizServerCore plugin, CommandLogConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        if (!isProtocolLibAvailable()) {
            plugin.getLogger().warning("[CommandLog] ProtocolLib não encontrado. Módulo desativado.");
            return;
        }

        protocolManager = ProtocolLibrary.getProtocolManager();

        packetListener = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.CHAT_COMMAND) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handleCommand(event);
            }
        };

        protocolManager.addPacketListener(packetListener);
        plugin.getLogger().info("[CommandLog] Módulo iniciado com ProtocolLib.");
    }

    public void stop() {
        if (protocolManager != null && packetListener != null) {
            protocolManager.removePacketListener(packetListener);
        }
    }

    public void reload() {
        config.reload();
    }

    private boolean isProtocolLibAvailable() {
        try {
            Class.forName("com.comphenix.protocol.ProtocolLibrary");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void handleCommand(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        String command = event.getPacket().getStrings().read(0);
        if (command == null || command.isEmpty()) return;

        // Comando com "/" no início
        if (!command.startsWith("/")) {
            command = "/" + command;
        }

        final String fullCommand = command;
        final String commandName = command.split(" ")[0].substring(1).toLowerCase();

        // Verificar se o jogador tem permissão
        boolean hasPermission = player.hasPermission("minecraft.command." + commandName) ||
                               player.hasPermission(commandName);

        // Verificar se é comando desconhecido
        boolean isUnknown = plugin.getServer().getPluginCommand(commandName) == null &&
                           !isVanillaCommand(commandName);

        // Determinar tipo e webhook
        String template;
        String webhookUrl;

        if (isUnknown) {
            template = "unknown-command";
            webhookUrl = config.getUnknownCommandWebhook();
        } else if (!hasPermission) {
            template = "no-permission";
            webhookUrl = config.getNoPermissionWebhook();
        } else {
            template = "executed";
            webhookUrl = config.getExecutedWebhook();
        }

        // Enviar webhook se URL configurada
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                sendWebhook(player, fullCommand, template, webhookUrl);
            }, config.getWaitTicks());
        }
    }

    private void sendWebhook(Player player, String command, String template, String webhookUrl) {
        String title = replacePlaceholders(config.getTemplateTitle(template), player, command);
        String description = replacePlaceholders(config.getTemplateDescription(template), player, command);
        String footer = config.getTemplateFooter(template);
        int color = config.getTemplateColor(template);

        // Construir embed
        JsonObject embed = WebhookUtil.buildEmbed(title, description, String.valueOf(color), null, footer);

        // Adicionar imagem se configurada
        if (config.isTemplateImageEnabled(template)) {
            String imageUrl = config.getTemplateImageUrl(template);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                JsonObject image = new JsonObject();
                image.addProperty("url", imageUrl);
                embed.add("image", image);
            }
        }

        // Adicionar thumbnail se configurada
        if (config.isTemplateThumbnailEnabled(template)) {
            String thumbnailUrl = config.getTemplateThumbnailUrl(template);
            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                thumbnailUrl = replacePlaceholders(thumbnailUrl, player, command);
                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", thumbnailUrl);
                embed.add("thumbnail", thumbnail);
            }
        }

        // Enviar webhook
        WebhookUtil.send(webhookUrl, "CommandLog", null, "", embed);
    }

    private String replacePlaceholders(String text, Player player, String command) {
        if (text == null) return "";

        String discordMention = getDiscordMention(player.getUniqueId());
        String group = getPlayerGroup(player);

        return text
            .replace("%player%", player.getName())
            .replace("%command%", command)
            .replace("%discord_mention%", discordMention)
            .replace("%group%", group);
    }

    private String getDiscordMention(UUID uuid) {
        // TODO: Integrar com DiscordSRV ou sistema de vinculação
        return "Não vinculado";
    }

    private String getPlayerGroup(Player player) {
        // TODO: Integrar com LuckPerms
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            // LuckPerms integration would go here
        } catch (ClassNotFoundException e) {
            // LuckPerms not available
        }
        return "default";
    }

    private boolean isVanillaCommand(String command) {
        // Comandos vanilla do Minecraft
        String[] vanillaCommands = {
            "give", "tp", "teleport", "kill", "effect", "enchant",
            "gamemode", "difficulty", "time", "weather", "time",
            "xp", "experience", "fill", "setblock", "summon",
            "teleport", "tp", "warp", "spawn", "back",
            "msg", "tell", "whisper", "reply", "r"
        };

        for (String vc : vanillaCommands) {
            if (vc.equalsIgnoreCase(command)) {
                return true;
            }
        }
        return false;
    }
}
