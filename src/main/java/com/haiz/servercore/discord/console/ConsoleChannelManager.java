package com.haiz.servercore.discord.console;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.config.ConfigManager;
import com.haiz.servercore.discord.webhook.WebhookUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.*;

public final class ConsoleChannelManager extends ListenerAdapter {
    private final HaizServerCore plugin;
    private TextChannel channel;
    private Handler handler;

    public ConsoleChannelManager(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        ConfigManager config = plugin.config();
        if (!config.isConsoleEnabled()) return;
        if (!plugin.discord().isOnline()) return;

        String channelId = config.consoleChannelId();
        if (channelId == null || channelId.isBlank()) return;

        Guild guild = plugin.discord().guild();
        if (guild == null) return;

        channel = guild.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("[Console] Canal não encontrado: " + channelId);
            return;
        }

        plugin.discord().addListener(this);
        startLogCapture();
        startCommandListener();

        plugin.getLogger().info("[Console] Console channel conectado.");
    }

    public void stop() {
        if (handler != null) {
            Logger.getLogger("").removeHandler(handler);
            handler.close();
            handler = null;
        }
        if (channel != null) {
            plugin.discord().removeListener(this);
            channel = null;
        }
    }

    private void startLogCapture() {
        ConfigManager config = plugin.config();
        String minLevel = config.consoleMinLevel();

        Level level = switch (minLevel.toUpperCase(java.util.Locale.ROOT)) {
            case "WARNING" -> Level.WARNING;
            case "SEVERE" -> Level.SEVERE;
            default -> Level.INFO;
        };

        handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() < level.intValue()) return;
                if (record.getLoggerName() != null && record.getLoggerName().startsWith("net.dv8tion.jda")) return;

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> sendToChannel(record));
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };

        handler.setLevel(level);
        Logger.getLogger("").addHandler(handler);
    }

    private void sendToChannel(LogRecord record) {
        if (channel == null || !channel.canTalk()) return;

        ConfigManager config = plugin.config();
        String format = config.consoleFormat();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        String date = LocalDateTime.now().format(dtf);
        String levelName = record.getLevel().getName();
        String loggerName = record.getLoggerName();
        String message = record.getMessage();

        String formatted = format
                .replace("{date}", date)
                .replace("{level}", levelName)
                .replace("{name}", loggerName)
                .replace("{message}", message);

        if (formatted.length() > 2000) {
            formatted = formatted.substring(0, 1997) + "...";
        }

        try {
            channel.sendMessage("```" + formatted + "```").queue(
                msg -> {},
                err -> plugin.getLogger().warning("[Console] Falha ao enviar log: " + err.getMessage())
            );
        } catch (Exception e) {
            plugin.getLogger().warning("[Console] Falha ao enviar log: " + e.getMessage());
        }
    }

    private void startCommandListener() {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (channel == null || !plugin.discord().isOnline()) {
                task.cancel();
                return;
            }
        }, 20L, 20L * 30);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (channel == null) return;
        if (!event.getChannel().getId().equals(channel.getId())) return;

        ConfigManager config = plugin.config();
        String prefix = config.consoleCommandPrefix();
        String message = event.getMessage().getContentRaw();

        if (!message.startsWith(prefix)) return;

        String command = message.substring(prefix.length()).trim();
        if (command.isEmpty()) return;

        String rootCmd = command.split("\\s+")[0].toLowerCase(Locale.ROOT);

        List<String> allowedCommands = config.consoleAllowedCommands();
        if (!allowedCommands.isEmpty()) {
            boolean allowed = false;
            for (String allowedCmd : allowedCommands) {
                if (rootCmd.equals(allowedCmd.toLowerCase(Locale.ROOT))) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                event.getChannel().sendMessage("Comando não permitido: `" + command + "`").queue();
                return;
            }
        } else {
            List<String> blockedCommands = config.consoleBlockedCommands();
            if (blockedCommands.contains(rootCmd)) {
                event.getChannel().sendMessage("Comando bloqueado: `" + command + "`").queue();
                return;
            }
        }

        String finalCommand = command;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                event.getChannel().sendMessage("Comando executado: `" + finalCommand + "`").queue();
            } catch (Exception e) {
                event.getChannel().sendMessage("Erro ao executar comando: " + e.getMessage()).queue();
            }
        });
    }
}
