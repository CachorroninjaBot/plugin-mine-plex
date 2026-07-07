package com.haiz.servercore.discord.commandlog;

import com.haiz.servercore.HaizServerCore;

public final class CommandLogModule {
    private final HaizServerCore plugin;
    private final CommandLogConfig config;
    private CommandLogListener listener;
    private boolean running = false;

    public CommandLogModule(HaizServerCore plugin) {
        this.plugin = plugin;
        this.config = new CommandLogConfig(plugin.getDataFolder());
    }

    public void start() {
        if (!config.isEnabled()) {
            plugin.getLogger().info("[CommandLog] Módulo desativado na configuração.");
            return;
        }

        listener = new CommandLogListener(plugin, config);
        listener.start();
        running = true;
    }

    public void stop() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
        running = false;
    }

    public void reload() {
        stop();
        config.reload();
        if (config.isEnabled()) {
            start();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public CommandLogConfig getConfig() {
        return config;
    }
}
