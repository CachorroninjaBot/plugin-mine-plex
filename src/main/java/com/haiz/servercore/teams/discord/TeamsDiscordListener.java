package com.haiz.servercore.teams.discord;

import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import com.haiz.servercore.vip.V2Messenger;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TeamsDiscordListener extends ListenerAdapter {

    private final TeamsModule module;

    public TeamsDiscordListener(TeamsModule module) {
        this.module = module;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!"time".equals(event.getName())) return;

        Optional<UUID> optUUID = module.plugin().vip().linkManager().uuidByDiscordId(event.getUser().getId());
        if (optUUID.isEmpty()) {
            event.reply("❌ Você precisa vincular sua conta Minecraft primeiro! Use `/verificar <nick>`.")
                    .setEphemeral(true).queue();
            return;
        }

        UUID mcUUID = optUUID.get();
        TeamsBridge bridge = module.bridge();
        Object team = bridge.getTeam(mcUUID);
        if (team == null) {
            event.reply("❌ Você não pertence a nenhum time no Minecraft.")
                    .setEphemeral(true).queue();
            return;
        }

        Object member = bridge.findMember(team, mcUUID);
        String rank = bridge.getPlayerRank(member);

        DataObject panel = TeamsEmbedFactory.buildTeamPanel(module, team, mcUUID, rank);

        event.reply("Use os botões abaixo para gerenciar seu time:")
                .setEphemeral(true).queue();

        V2Messenger.replyInteraction(event.getJDA(), event.getHook().getInteraction().getToken(), panel)
                .thenAccept(status -> {
                    if (status < 200 || status >= 300) {
                        module.plugin().getLogger().warning("[Teams/Discord] Falha ao enviar painel V2: HTTP " + status);
                    }
                });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        if (!"teams".equals(parts[0])) return;

        Optional<UUID> optUUID = module.plugin().vip().linkManager().uuidByDiscordId(event.getUser().getId());
        if (optUUID.isEmpty()) {
            event.reply("❌ Conta não vinculada.").setEphemeral(true).queue();
            return;
        }

        UUID mcUUID = optUUID.get();
        TeamsBridge bridge = module.bridge();
        Object team = bridge.getTeam(mcUUID);
        if (team == null) {
            event.reply("❌ Você não pertence a nenhum time.").setEphemeral(true).queue();
            return;
        }

        Object member = bridge.findMember(team, mcUUID);
        String viewerRank = member != null ? bridge.getPlayerRank(member) : "DEFAULT";

        String action = parts.length > 1 ? parts[1] : "panel";

        switch (action) {
            case "panel" -> {
                DataObject panel = TeamsEmbedFactory.buildTeamPanel(module, team, mcUUID, viewerRank);
                event.editMessage(" ").setComponents().queue();
                V2Messenger.replyInteraction(event.getJDA(), event.getHook().getInteraction().getToken(), panel);
            }
            case "members" -> {
                DataObject membersPanel = TeamsEmbedFactory.buildMembersPanel(module, team, viewerRank);
                event.editMessage(" ").setComponents().queue();
                V2Messenger.replyInteraction(event.getJDA(), event.getHook().getInteraction().getToken(), membersPanel);
            }
            case "bank" -> {
                DataObject bankPanel = TeamsEmbedFactory.buildBankPanel(module, team, viewerRank);
                event.editMessage(" ").setComponents().queue();
                V2Messenger.replyInteraction(event.getJDA(), event.getHook().getInteraction().getToken(), bankPanel);
            }
            case "warps" -> {
                StringBuilder sb = new StringBuilder("**Warps do Time:**\n");
                var warps = bridge.getWarps(team);
                if (warps.isEmpty()) {
                    sb.append("_Nenhuma warp definida._");
                } else {
                    warps.forEach((name, warp) -> {
                        var loc = bridge.getWarpLocation(warp);
                        sb.append("• **").append(name).append("**");
                        if (loc != null && loc.getWorld() != null) {
                            sb.append(" — `").append(loc.getWorld().getName())
                                    .append(" ").append(String.format("%.0f", loc.getX()))
                                    .append(", ").append(String.format("%.0f", loc.getY()))
                                    .append(", ").append(String.format("%.0f", loc.getZ())).append("`");
                        }
                        sb.append("\n");
                    });
                }
                event.reply(sb.toString()).setEphemeral(true).queue();
            }
            case "allies" -> {
                StringBuilder sb = new StringBuilder("**Alianças:**\n");
                Set<UUID> allies = bridge.getAllies(team);
                if (allies.isEmpty()) {
                    sb.append("_Nenhuma aliança._");
                } else {
                    for (UUID allyId : allies) {
                        sb.append("• **").append(bridge.getAllyName(allyId)).append("**\n");
                    }
                }
                event.reply(sb.toString()).setEphemeral(true).queue();
            }
            case "settings" -> {
                if (!"OWNER".equals(viewerRank)) {
                    event.reply("❌ Apenas o dono pode acessar configurações.").setEphemeral(true).queue();
                    return;
                }
                String settings = String.format(
                        "**Configurações do Time:**\n" +
                        "• Nome: `%s`\n" +
                        "• Tag: `%s`\n" +
                        "• Descrição: `%s`\n" +
                        "• Aberto: %s\n" +
                        "• PvP: %s\n" +
                        "• Cor: %s",
                        bridge.getTeamName(team),
                        bridge.getTeamTag(team),
                        bridge.getTeamDescription(team),
                        bridge.isTeamOpen(team) ? "Sim" : "Não",
                        bridge.isTeamPvp(team) ? "Sim" : "Não",
                        bridge.getTeamColor(team)
                );
                event.reply(settings).setEphemeral(true).queue();
            }
            case "level" -> {
                String info = String.format(
                        "**Level do Time:**\n" +
                        "• Level: **%d**\n" +
                        "• Score: **%.0f**\n" +
                        "• Banco: **$%.2f**",
                        bridge.getTeamLevel(team),
                        bridge.getTeamScore(team),
                        bridge.getTeamMoney(team)
                );
                event.reply(info).setEphemeral(true).queue();
            }
            case "deposit" -> {
                event.reply("💰 Use o comando no Minecraft para depositar: `/team deposit <valor>`")
                        .setEphemeral(true).queue();
            }
            case "withdraw" -> {
                if (!"OWNER".equals(viewerRank) && !"ADMIN".equals(viewerRank)) {
                    event.reply("❌ Sem permissão.").setEphemeral(true).queue();
                    return;
                }
                event.reply("💰 Use o comando no Minecraft para sacar: `/team withdraw <valor>`")
                        .setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getComponentId().startsWith("teams:")) return;
        event.reply("Selecione uma opção válida.").setEphemeral(true).queue();
    }
}
