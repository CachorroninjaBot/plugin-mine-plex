package com.haiz.servercore.teams.discord;

import com.haiz.servercore.HaizServerCore;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.List;

public final class TeamsSlashCommands {

    private TeamsSlashCommands() {}

    public static void register(HaizServerCore plugin) {
        if (!plugin.discord().isOnline()) return;
        var jda = plugin.discord().jda();
        String guildId = plugin.config().guildId();

        var time = Commands.slash("time", "Gerencie seu time no Minecraft");

        var commands = List.of(time);

        if (guildId != null && !guildId.isBlank()) {
            Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands().addCommands(commands).queue(
                        success -> plugin.getLogger().info("[Teams/Discord] Comando /time registrado na guild: " + guild.getName()),
                        error -> plugin.getLogger().warning("[Teams/Discord] Falha ao registrar comando: " + error.getMessage())
                );
                return;
            }
        }

        jda.updateCommands().addCommands(commands).queue(
                success -> plugin.getLogger().info("[Teams/Discord] Comando /time registrado globalmente"),
                error -> plugin.getLogger().warning("[Teams/Discord] Falha ao registrar comando: " + error.getMessage())
        );
    }
}
