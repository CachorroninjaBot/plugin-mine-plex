package com.haiz.servercore.discord.responses;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.config.ConfigManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public final class CannedResponseManager extends ListenerAdapter {
    private final HaizServerCore plugin;
    private Map<String, String> responses = new HashMap<>();

    public CannedResponseManager(HaizServerCore plugin) {
        this.plugin = plugin;
        loadResponses();
    }

    public void start() {
        ConfigManager config = plugin.config();
        if (!config.isCannedResponsesEnabled()) return;
        if (!plugin.discord().isOnline()) return;

        plugin.discord().addListener(this);
        plugin.getLogger().info("[CannedResponses] " + responses.size() + " respostas carregadas.");
    }

    public void stop() {
        plugin.discord().removeListener(this);
        responses.clear();
    }

    public void reload() {
        stop();
        loadResponses();
        start();
    }

    private void loadResponses() {
        responses.clear();
        try {
            var config = plugin.config().getModuleConfig("responses").getConfigurationSection("responses");
            if (config != null) {
                for (String key : config.getKeys(false)) {
                    String value = config.getString(key);
                    if (value != null && !value.isBlank()) {
                        responses.put(key.toLowerCase(java.util.Locale.ROOT), value);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[CannedResponses] Falha ao carregar respostas: " + e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        ConfigManager config = plugin.config();
        if (!config.isCannedResponsesEnabled()) return;

        String message = event.getMessage().getContentRaw().trim().toLowerCase(java.util.Locale.ROOT);

        String response = responses.get(message);
        if (response == null) return;

        response = response
                .replace("%playercount%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%playermax%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%serverversion%", Bukkit.getVersion())
                .replace("%motd%", Bukkit.getServer().getMotd());

        String finalResponse = response;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                event.getChannel().sendMessage(finalResponse).queue();
            } catch (Exception e) {
                plugin.getLogger().warning("[CannedResponses] Falha ao enviar resposta: " + e.getMessage());
            }
        });
    }
}
