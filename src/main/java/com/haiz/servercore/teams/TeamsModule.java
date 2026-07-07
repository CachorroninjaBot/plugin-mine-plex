package com.haiz.servercore.teams;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.teams.discord.TeamsDiscordListener;
import com.haiz.servercore.teams.gui.TeamGUIListener;
import com.haiz.servercore.teams.gui.TeamMainMenu;
import com.haiz.servercore.teams.web.WebServer;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

import java.util.ArrayList;
import java.util.List;

public final class TeamsModule {

    private final HaizServerCore plugin;
    private TeamsConfig teamsConfig;
    private TeamsBridge bridge;
    private WebServer webServer;
    private TeamsDiscordListener discordListener;
    private boolean running;

    public TeamsModule(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.teamsConfig = new TeamsConfig(plugin.config().getModuleConfig("teams"));
        if (!teamsConfig.enabled()) {
            plugin.getLogger().info("[Teams] Módulo desativado no config.yml.");
            return;
        }

        this.bridge = new TeamsBridge();
        if (!bridge.initialize()) {
            plugin.getLogger().warning("[Teams] BetterTeams não encontrado. Módulo desativado.");
            return;
        }

        registerMinecraftCommands();
        registerMinecraftGUIListener();

        if (teamsConfig.webEnabled()) {
            this.webServer = new WebServer(this);
            webServer.start();
        }

        scheduleDiscordSetup();

        running = true;
        plugin.getLogger().info("[Teams] Módulo de times iniciado. Web=" + (teamsConfig.webEnabled() ? "on" : "off"));
    }

    public void stop() {
        if (webServer != null) {
            webServer.stop();
            webServer = null;
        }
        if (discordListener != null && plugin.discord().jda() != null) {
            plugin.discord().jda().removeEventListener(discordListener);
            discordListener = null;
        }
        bridge = null;
        teamsConfig = null;
        running = false;
    }

    public void reload() {
        stop();
        start();
    }

    private void registerMinecraftCommands() {
        TeamMenuCommand executor = new TeamMenuCommand(this);
        PluginCommand cmd = plugin.getCommand("time");
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }
    }

    private void registerMinecraftGUIListener() {
        Bukkit.getPluginManager().registerEvents(new TeamGUIListener(this), plugin);
    }

    private void scheduleDiscordSetup() {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (plugin.discord().isOnline()) {
                registerDiscordListener();
                task.cancel();
            } else if (!running) {
                task.cancel();
            }
        }, 20L, 40L);
    }

    public List<SlashCommandData> getSlashCommands() {
        List<SlashCommandData> commands = new ArrayList<>();
        commands.add(net.dv8tion.jda.api.interactions.commands.build.Commands.slash("time", "Gerencie seu time no Minecraft"));
        return commands;
    }

    private void registerDiscordListener() {
        if (!plugin.discord().isOnline()) return;
        if (discordListener != null) {
            plugin.discord().jda().removeEventListener(discordListener);
        }
        discordListener = new TeamsDiscordListener(this);
        plugin.discord().jda().addEventListener(discordListener);
    }

    public HaizServerCore plugin() { return plugin; }
    public TeamsConfig teamsConfig() { return teamsConfig; }
    public TeamsBridge bridge() { return bridge; }
    public boolean isRunning() { return running; }
}
