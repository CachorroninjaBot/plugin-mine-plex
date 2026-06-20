package com.haiz.servercore.metrics;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.discord.DiscordEmbedFactory;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EngagementTracker {
    private final HaizServerCore plugin;
    private final Deque<Long> recentJoins = new ArrayDeque<>();
    private final Deque<Long> recentLeaves = new ArrayDeque<>();
    private final Map<UUID, Deque<Long>> recentCommands = new ConcurrentHashMap<>();

    public EngagementTracker(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public synchronized void recordJoin(Player player) {
        long now = Instant.now().getEpochSecond();
        recentJoins.addLast(now);
        prune(recentJoins, now, plugin.config().massJoinWindowSeconds());
        if (plugin.config().isMassJoinAlertEnabled() && recentJoins.size() >= plugin.config().massJoinAmount()) {
            plugin.discordLogs().alert(DiscordEmbedFactory.alert("Entrada em massa",
                    recentJoins.size() + " jogadores entraram nos ultimos " + plugin.config().massJoinWindowSeconds() + "s."));
            recentJoins.clear();
        }
    }

    public synchronized void recordLeave(Player player) {
        long now = Instant.now().getEpochSecond();
        recentLeaves.addLast(now);
        prune(recentLeaves, now, plugin.config().massLeaveWindowSeconds());
        if (plugin.config().isMassLeaveAlertEnabled() && recentLeaves.size() >= plugin.config().massLeaveAmount()) {
            plugin.discordLogs().alert(DiscordEmbedFactory.alert("Saida em massa",
                    recentLeaves.size() + " jogadores sairam nos ultimos " + plugin.config().massLeaveWindowSeconds() + "s."));
            recentLeaves.clear();
        }
    }

    public void recordCommand(Player player, String command) {
        if (!plugin.config().isCommandSpamAlertEnabled()) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        Deque<Long> commands = recentCommands.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        synchronized (commands) {
            commands.addLast(now);
            prune(commands, now, plugin.config().commandSpamWindowSeconds());
            if (commands.size() >= plugin.config().commandSpamAmount()) {
                plugin.discordLogs().alert(DiscordEmbedFactory.alert("Possivel spam de comandos",
                        player.getName() + " executou " + commands.size() + " comandos em "
                                + plugin.config().commandSpamWindowSeconds() + "s. Ultimo: `" + command + "`"));
                commands.clear();
            }
        }
    }

    private void prune(Deque<Long> deque, long now, long windowSeconds) {
        while (!deque.isEmpty() && now - deque.peekFirst() > windowSeconds) {
            deque.removeFirst();
        }
    }
}
