package com.haiz.servercore.discord;

import com.haiz.servercore.HaizServerCore;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DiscordLogService {
    private final HaizServerCore plugin;
    private final BlockingQueue<QueuedEmbed> queue;
    private final ScheduledExecutorService executor;
    private volatile JDA jda;
    private volatile boolean running = true;

    public DiscordLogService(HaizServerCore plugin) {
        this.plugin = plugin;
        this.queue = new LinkedBlockingQueue<>(plugin.config().discordMaxBufferSize());
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "HaizServerCore-DiscordQueue");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::flushOne, 1, plugin.config().discordQueueDelayMillis(), TimeUnit.MILLISECONDS);
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    public void serverLog(MessageEmbed embed) {
        serverLog(DiscordLogMessage.of(embed));
    }

    public void serverLog(DiscordLogMessage message) {
        enqueue("server-logs", message);
    }

    public void commandLog(MessageEmbed embed) {
        commandLog(DiscordLogMessage.of(embed));
    }

    public void commandLog(DiscordLogMessage message) {
        enqueue("command-logs", message);
    }

    public void joinLeave(MessageEmbed embed) {
        joinLeave(DiscordLogMessage.of(embed));
    }

    public void joinLeave(DiscordLogMessage message) {
        enqueue("join-leave-logs", message);
    }

    public void joinLeaveNow(DiscordLogMessage message) {
        sendNow("join-leave-logs", message);
    }

    public void consoleLog(MessageEmbed embed) {
        enqueue("console-logs", DiscordLogMessage.of(embed));
    }

    public void metricsReport(MessageEmbed embed) {
        enqueue("metrics-reports", DiscordLogMessage.of(embed));
    }

    public void alert(MessageEmbed embed) {
        enqueue("alerts", DiscordLogMessage.of(embed));
    }

    public void testAllChannels() {
        enqueue("server-logs", DiscordEmbedFactory.info("Teste HaizServerCore", "Canal de logs do servidor OK."));
        enqueue("command-logs", DiscordEmbedFactory.playerCommand("Steve", "default", "/haizcore testdiscord"));
        enqueue("join-leave-logs", DiscordEmbedFactory.join("Steve", null, false, 1));
        enqueue("console-logs", DiscordEmbedFactory.info("Console", "Canal de console OK."));
        enqueue("metrics-reports", DiscordEmbedFactory.metrics("Metricas", "Canal de relatorios OK."));
        enqueue("alerts", DiscordEmbedFactory.alert("Teste", "Canal de alertas OK."));
    }

    public void stop() {
        running = false;
        executor.shutdownNow();
        queue.clear();
    }

    private void enqueue(String channelKey, MessageEmbed embed) {
        enqueue(channelKey, DiscordLogMessage.of(embed));
    }

    private void enqueue(String channelKey, DiscordLogMessage message) {
        Objects.requireNonNull(message, "message");
        String channelId = plugin.config().channelId(channelKey);
        if (channelId == null || channelId.isBlank()) {
            return;
        }
        if (!queue.offer(new QueuedEmbed(channelId, message))) {
            plugin.getLogger().warning("Buffer Discord cheio; log descartado para evitar spam/travamento.");
        }
    }

    private void sendNow(String channelKey, DiscordLogMessage message) {
        Objects.requireNonNull(message, "message");
        String channelId = plugin.config().channelId(channelKey);
        JDA current = jda;
        if (current == null || channelId == null || channelId.isBlank()) {
            return;
        }
        TextChannel channel = current.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Canal Discord nao encontrado: " + channelId);
            return;
        }
        try {
            createAction(channel, message).complete();
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Falha ao enviar log Discord imediato: " + exception.getMessage());
        }
    }

    private void flushOne() {
        if (!running) {
            return;
        }
        JDA current = jda;
        if (current == null) {
            return;
        }
        QueuedEmbed item = queue.poll();
        if (item == null) {
            return;
        }
        TextChannel channel = current.getTextChannelById(item.channelId());
        if (channel == null) {
            plugin.getLogger().warning("Canal Discord nao encontrado: " + item.channelId());
            return;
        }
        createAction(channel, item.message()).queue(
                ignored -> {
                },
                error -> plugin.getLogger().warning("Falha ao enviar embed Discord: " + error.getMessage())
        );
    }

    private MessageCreateAction createAction(TextChannel channel, DiscordLogMessage message) {
        MessageCreateAction action = channel.sendMessageEmbeds(message.embed());
        if (!message.components().isEmpty()) {
            action.setComponents(message.components());
        }
        return action;
    }

    private record QueuedEmbed(String channelId, DiscordLogMessage message) {
    }
}
