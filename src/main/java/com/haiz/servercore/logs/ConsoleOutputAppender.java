package com.haiz.servercore.logs;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.discord.DiscordEmbedFactory;
import com.haiz.servercore.discord.DiscordLogService;
import com.haiz.servercore.utils.TextUtils;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConsoleOutputAppender extends AbstractAppender {
    private final HaizServerCore plugin;
    private final DiscordLogService discordLogService;
    private final AtomicBoolean attached = new AtomicBoolean();

    public ConsoleOutputAppender(HaizServerCore plugin, DiscordLogService discordLogService) {
        super("HaizServerCoreConsoleOutput", null, PatternLayout.newBuilder().withPattern("[%level] %msg").build(), true, Property.EMPTY_ARRAY);
        this.plugin = plugin;
        this.discordLogService = discordLogService;
    }

    public void startCapture() {
        if (!attached.compareAndSet(false, true)) {
            return;
        }
        try {
            start();
            LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
            context.getRootLogger().addAppender(this);
            context.updateLoggers();
            plugin.getLogger().info("Captura opcional de output do console ativada.");
        } catch (Exception exception) {
            attached.set(false);
            plugin.getLogger().warning("Nao foi possivel anexar ConsoleOutputAppender: " + exception.getMessage());
        }
    }

    public void stopCapture() {
        if (!attached.compareAndSet(true, false)) {
            return;
        }
        try {
            LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
            context.getRootLogger().removeAppender(this);
            context.updateLoggers();
            stop();
        } catch (Exception exception) {
            plugin.getLogger().warning("Erro ao remover ConsoleOutputAppender: " + exception.getMessage());
        }
    }

    @Override
    public void append(LogEvent event) {
        if (!attached.get() || event == null || event.getLoggerName().startsWith("com.haiz.servercore")) {
            return;
        }
        String rendered = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
        if (rendered.isBlank()) {
            return;
        }
        discordLogService.consoleLog(DiscordEmbedFactory.info("Console " + event.getLevel().name(), TextUtils.trimForDiscord(rendered, 1500)));
    }
}
