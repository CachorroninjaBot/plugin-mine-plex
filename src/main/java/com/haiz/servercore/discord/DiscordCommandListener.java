package com.haiz.servercore.discord;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.metrics.MetricsSnapshot;
import com.haiz.servercore.metrics.PlayerStats;
import com.haiz.servercore.utils.TimeUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DiscordCommandListener extends ListenerAdapter {
    private final HaizServerCore plugin;

    public DiscordCommandListener(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public static List<CommandData> commandData() {
        return List.of(
                Commands.slash("status", "Mostra o status do servidor Minecraft"),
                Commands.slash("online", "Lista jogadores online"),
                Commands.slash("top", "Mostra rankings")
                        .addSubcommands(
                                new SubcommandData("playtime", "Top jogadores por tempo online"),
                                new SubcommandData("commands", "Top jogadores por comandos")
                        ),
                Commands.slash("metrics", "Mostra metricas")
                        .addSubcommands(
                                new SubcommandData("today", "Metricas de hoje"),
                                new SubcommandData("week", "Metricas semanais")
                        )
        );
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "status" -> status(event);
            case "online" -> online(event);
            case "top" -> top(event);
            case "metrics" -> metrics(event);
            default -> event.reply("Comando desconhecido.").setEphemeral(true).queue();
        }
    }

    private void status(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            MetricsSnapshot snapshot = plugin.metrics().snapshot();
            hook.sendMessageEmbeds(DiscordEmbedFactory.metrics("Status do servidor",
                    "Online: " + snapshot.onlinePlayers() + "/" + snapshot.maxPlayers()
                            + "\nTPS: " + (snapshot.tps() < 0 ? "indisponivel" : String.format(java.util.Locale.US, "%.2f", snapshot.tps()))
                            + "\nMSPT: " + String.format(java.util.Locale.US, "%.1f", snapshot.mspt())
                            + "\nAtivos/AFK: " + snapshot.activePlayers() + "/" + snapshot.afkPlayers()
                            + "\nMemoria: " + snapshot.usedMemoryMb() + "/" + snapshot.maxMemoryMb() + " MB"
                            + "\nDiscord: " + plugin.discord().getStateLabel()
                            + "\nDatabase: " + plugin.database().getStateLabel())).queue();
        }));
    }

    private void online(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            String players = org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("Nenhum jogador online.");
            hook.sendMessage("Online (" + org.bukkit.Bukkit.getOnlinePlayers().size() + "): " + players).queue();
        }));
    }

    private void top(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> java.util.concurrent.CompletableFuture.runAsync(() -> {
            String sub = event.getSubcommandName();
            String column = "commands".equals(sub) ? "total_commands" : "total_playtime_seconds";
            List<PlayerStats> top = plugin.database().players().top(column, 5);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < top.size(); i++) {
                PlayerStats stats = top.get(i);
                String value = "commands".equals(sub) ? stats.totalCommands() + " comandos" : TimeUtils.humanDuration(stats.totalPlaytimeSeconds());
                builder.append(i + 1).append(". ").append(stats.name()).append(" - ").append(value).append('\n');
            }
            hook.sendMessage(builder.isEmpty() ? "Sem dados ainda." : builder.toString()).queue();
        }));
    }

    private void metrics(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            MetricsSnapshot snapshot = plugin.metrics().snapshot();
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                if ("week".equals(event.getSubcommandName())) {
                    hook.sendMessageEmbeds(plugin.metrics().reports().weeklyReport(snapshot)).queue();
                } else {
                    hook.sendMessageEmbeds(plugin.metrics().reports().dailyReport(snapshot)).queue();
                }
            });
        }));
    }
}
