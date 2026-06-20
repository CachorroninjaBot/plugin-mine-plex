package com.haiz.servercore.discord;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import com.haiz.servercore.HaizServerCore;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;

public final class DiscordBotManager {
    private final HaizServerCore plugin;
    private final DiscordLogService logService;
    private volatile JDA jda;
    private volatile State state = State.DISABLED;

    public DiscordBotManager(HaizServerCore plugin, DiscordLogService logService) {
        this.plugin = plugin;
        this.logService = logService;
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
                    .warning("Token Discord vazio/placeholder. Modulo Discord desativado sem parar o plugin.");
            return;
        }
        state = State.STARTING;
        CompletableFuture.runAsync(() -> {
            try {
                JDABuilder builder = JDABuilder.createDefault(token, EnumSet.of(GatewayIntent.GUILD_MESSAGES));
                builder.setStatus(OnlineStatus.ONLINE);
                if (plugin.config().isActivityEnabled()) {
                    builder.setActivity(Activity.playing(plugin.config().activityText()));
                }
                builder.addEventListeners(new DiscordCommandListener(plugin));
                JDA built = builder.build();
                built.awaitReady();
                this.jda = built;
                logService.setJda(built);
                registerCommands(built);
                state = State.ONLINE;
                plugin.getLogger().info("Discord conectado como " + built.getSelfUser().getAsTag() + ".");
                validateChannels(built);
            } catch (Exception exception) {
                state = State.ERROR;
                plugin.getLogger()
                        .warning("Discord falhou ao iniciar. Token nao sera exibido. Erro: " + exception.getMessage());
            }
        });
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
        logService.setJda(null);
        state = State.DISABLED;
    }

    // já existe isOnline(), adicione:
    public JDA jda() {
        return jda;
    }

    public boolean isOnline() {
        return state == State.ONLINE && jda != null;

    }

    public String getStateLabel() {
        return state.name().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean isUsableToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String normalized = token.trim();
        return !normalized.equalsIgnoreCase("COLOQUE_O_TOKEN_AQUI") && normalized.length() > 40;
    }

    private void registerCommands(JDA jda) {
        String guildId = plugin.config().guildId();
        if (guildId == null || guildId.isBlank()) {
            jda.updateCommands()
                    .addCommands(DiscordCommandListener.commandData())
                    .queue();
            return;
        }
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            plugin.getLogger().warning(
                    "Guild Discord nao encontrada para guild-id=" + guildId + ". Slash commands globais serao usados.");
            jda.updateCommands().addCommands(DiscordCommandListener.commandData()).queue();
            return;
        }
        guild.updateCommands()
                .addCommands(DiscordCommandListener.commandData())
                .queue();
    }

    private void validateChannels(JDA jda) {
        for (String key : new String[] { "server-logs", "command-logs", "join-leave-logs", "console-logs",
                "metrics-reports", "alerts" }) {
            String channelId = plugin.config().channelId(key);
            if (channelId != null && !channelId.isBlank() && jda.getTextChannelById(channelId) == null) {
                plugin.getLogger().warning("Canal Discord configurado nao foi encontrado: " + key + "=" + channelId);
            }
        }
    }

    private enum State {
        DISABLED,
        STARTING,
        ONLINE,
        ERROR
    }
}
