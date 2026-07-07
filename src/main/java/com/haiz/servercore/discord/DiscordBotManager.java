package com.haiz.servercore.discord;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import com.haiz.servercore.HaizServerCore;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

public final class DiscordBotManager {
    private final HaizServerCore plugin;
    private volatile JDA jda;
    private volatile State state = State.DISABLED;

    public DiscordBotManager(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.config().isDiscordEnabled()) {
            state = State.DISABLED;
            plugin.getLogger().info("Discord desativado no config.yml.");
            return;
        }
        String token = plugin.config().discordToken();
        if (!isUsableToken(token)) {
            state = State.DISABLED;
            plugin.getLogger()
                    .warning("Token Discord vazio/placeholder. Modulo Discord desativado.");
            return;
        }
        state = State.STARTING;
        CompletableFuture.runAsync(() -> {
            try {
                EnumSet<GatewayIntent> intents = EnumSet.of(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.MESSAGE_CONTENT
                );

                JDABuilder builder = JDABuilder.createDefault(token, intents);
                builder.setStatus(OnlineStatus.ONLINE);

                if (plugin.config().isActivityEnabled()) {
                    String activityType = plugin.config().activityType();
                    String activityText = plugin.config().activityText();
                    Activity activity = switch (activityType.toUpperCase(java.util.Locale.ROOT)) {
                        case "WATCHING" -> Activity.watching(activityText);
                        case "LISTENING" -> Activity.listening(activityText);
                        case "COMPETING" -> Activity.competing(activityText);
                        default -> Activity.playing(activityText);
                    };
                    builder.setActivity(activity);
                }

                JDA built = builder.build();
                built.awaitReady();
                this.jda = built;
                state = State.ONLINE;
                plugin.getLogger().info("Discord conectado como " + built.getSelfUser().getAsTag() + ".");
            } catch (Exception exception) {
                state = State.ERROR;
                plugin.getLogger()
                        .warning("Discord falhou ao iniciar. Erro: " + exception.getMessage());
            }
        });
    }

    public void addListener(ListenerAdapter listener) {
        JDA current = jda;
        if (current != null) {
            current.addEventListener(listener);
        }
    }

    public void removeListener(ListenerAdapter listener) {
        JDA current = jda;
        if (current != null) {
            current.removeEventListener(listener);
        }
    }

    public void reload() {
        stop();
        start();
    }

    public void stop() {
        JDA current = jda;
        if (current != null) {
            current.shutdownNow();
        }
        jda = null;
        state = State.DISABLED;
    }

    public JDA jda() { return jda; }

    public boolean isOnline() { return state == State.ONLINE && jda != null; }

    public String getStateLabel() { return state.name().toLowerCase(java.util.Locale.ROOT); }

    private boolean isUsableToken(String token) {
        if (token == null || token.isBlank()) return false;
        String normalized = token.trim();
        return !normalized.equalsIgnoreCase("COLOQUE_O_TOKEN_AQUI") && normalized.length() > 40;
    }

    private enum State {
        DISABLED, STARTING, ONLINE, ERROR
    }
}
