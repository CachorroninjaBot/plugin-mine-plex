package com.haiz.servercore.teams.discord;

import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import com.haiz.servercore.vip.V2MessageBuilder;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TeamsEmbedFactory {

    private TeamsEmbedFactory() {}

    public static DataObject buildTeamPanel(TeamsModule module, Object team, UUID viewerUUID, String viewerRank) {
        TeamsBridge bridge = module.bridge();
        boolean isOwner = "OWNER".equals(viewerRank);
        boolean isAdmin = "ADMIN".equals(viewerRank);

        String colorHex = bridge.getTeamColor(team).toUpperCase();
        int accentColor = getColorInt(colorHex);

        V2MessageBuilder builder = V2MessageBuilder.create();

        builder.addContainer(accentColor, children -> {
            children.add(V2MessageBuilder.separator(false));

            children.add(DataObject.empty()
                    .put("type", 10)
                    .put("content", "# " + getRankEmoji(viewerRank) + " " + bridge.getTeamName(team)));

            children.add(DataObject.empty()
                    .put("type", 10)
                    .put("content", "**Tag:** `" + bridge.getTeamTag(team) + "` | **Level:** " + bridge.getTeamLevel(team) + " | **Score:** " + String.format("%.0f", bridge.getTeamScore(team))));

            children.add(DataObject.empty()
                    .put("type", 10)
                    .put("content", "**Membros:** " + bridge.getMembers(team).size() + " | **Banco:** $" + String.format("%.2f", bridge.getTeamMoney(team))));

            String desc = bridge.getTeamDescription(team);
            if (desc != null && !desc.isEmpty()) {
                children.add(V2MessageBuilder.separator(false));
                children.add(DataObject.empty()
                        .put("type", 10)
                        .put("content", "> " + desc));
            }

            children.add(V2MessageBuilder.separator(true));

            List<DataObject> buttons = new ArrayList<>();
            buttons.add(V2MessageBuilder.button(1, "teams:members:" + viewerRank, "Membros"));
            buttons.add(V2MessageBuilder.button(1, "teams:warps:" + viewerRank, "Warps"));
            buttons.add(V2MessageBuilder.button(1, "teams:allies:" + viewerRank, "Aliados"));
            buttons.add(V2MessageBuilder.button(1, "teams:bank:" + viewerRank, "Banco"));

            children.add(V2MessageBuilder.actionRow(buttons.toArray(new DataObject[0])));

            if (isOwner) {
                List<DataObject> ownerButtons = new ArrayList<>();
                ownerButtons.add(V2MessageBuilder.button(3, "teams:settings:" + viewerRank, "Configurações"));
                ownerButtons.add(V2MessageBuilder.button(2, "teams:level:" + viewerRank, "Level Up"));
                children.add(V2MessageBuilder.actionRow(ownerButtons.toArray(new DataObject[0])));
            } else if (isAdmin) {
                List<DataObject> adminButtons = new ArrayList<>();
                adminButtons.add(V2MessageBuilder.button(3, "teams:settings:" + viewerRank, "Configurações"));
                children.add(V2MessageBuilder.actionRow(adminButtons.toArray(new DataObject[0])));
            }
        });

        return builder.build();
    }

    public static DataObject buildMembersPanel(TeamsModule module, Object team, String viewerRank) {
        TeamsBridge bridge = module.bridge();
        boolean isOwner = "OWNER".equals(viewerRank);

        V2MessageBuilder builder = V2MessageBuilder.create();

        builder.addContainer(0x5865F2, children -> {
            children.add(DataObject.empty()
                    .put("type", 10)
                    .put("content", "# Membros do Time"));

            children.add(V2MessageBuilder.separator(false));

            StringBuilder membersList = new StringBuilder();
            for (Object member : bridge.getMembers(team)) {
                UUID uuid = bridge.getPlayerUUID(member);
                String rank = bridge.getPlayerRank(member);
                String emoji = switch (rank) {
                    case "OWNER" -> "遆";
                    case "ADMIN" -> "遆";
                    default -> "•";
                };
                String name = bridge.resolvePlayerName(uuid);
                boolean online = module.plugin().getServer().getPlayer(uuid) != null;
                membersList.append(emoji).append(" **").append(name).append("** — ").append(rank);
                if (online) membersList.append(" 🟢");
                membersList.append("\n");
            }

            children.add(DataObject.empty()
                    .put("type", 10)
                    .put("content", membersList.toString()));

            children.add(V2MessageBuilder.separator(true));

            List<DataObject> buttons = new ArrayList<>();
            buttons.add(V2MessageBuilder.button(5, "teams:panel:" + viewerRank, "Voltar"));
            children.add(V2MessageBuilder.actionRow(buttons.toArray(new DataObject[0])));
        });

        return builder.build();
    }

    public static DataObject buildBankPanel(TeamsModule module, Object team, String viewerRank) {
        TeamsBridge bridge = module.bridge();
        boolean isOwner = "OWNER".equals(viewerRank);
        boolean isAdmin = "ADMIN".equals(viewerRank);

        V2MessageBuilder builder = V2MessageBuilder.create();

        builder.addContainer(0x57F287, children -> {
            children.add(DataObject.empty()
                    .put("type", 10)
                    .put("content", "# Banco do Time"));

            children.add(V2MessageBuilder.separator(false));

            children.add(DataObject.empty()
                    .put("type", 10)
                    .put("content", "**Saldo:** $" + String.format("%.2f", bridge.getTeamMoney(team))));

            children.add(DataObject.empty()
                    .put("type", 10)
                    .put("content", "**Score:** " + String.format("%.0f", bridge.getTeamScore(team))));

            children.add(DataObject.empty()
                    .put("type", 10)
                    .put("content", "**Level:** " + bridge.getTeamLevel(team)));

            children.add(V2MessageBuilder.separator(true));

            List<DataObject> buttons = new ArrayList<>();
            buttons.add(V2MessageBuilder.button(3, "teams:deposit:" + viewerRank, "Depositar"));
            if (isOwner || isAdmin) {
                buttons.add(V2MessageBuilder.button(4, "teams:withdraw:" + viewerRank, "Sacar"));
            }
            buttons.add(V2MessageBuilder.button(5, "teams:panel:" + viewerRank, "Voltar"));
            children.add(V2MessageBuilder.actionRow(buttons.toArray(new DataObject[0])));
        });

        return builder.build();
    }

    private static int getColorInt(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "RED" -> 0xED4245;
            case "GREEN" -> 0x57F287;
            case "BLUE" -> 0x5865F2;
            case "YELLOW" -> 0xFEE75C;
            case "GOLD" -> 0xF1C40F;
            case "AQUA" -> 0x1ABC9C;
            case "LIGHT_PURPLE" -> 0xEB459E;
            case "DARK_PURPLE" -> 0x9B59B6;
            case "DARK_RED" -> 0xA84300;
            case "DARK_BLUE" -> 0x206694;
            case "DARK_GREEN" -> 0x1F8B4C;
            case "DARK_AQUA" -> 0x11806A;
            case "DARK_GRAY" -> 0x607D8B;
            case "GRAY" -> 0x95A5A6;
            case "BLACK" -> 0x2C2F33;
            default -> 0xFFFFFF;
        };
    }

    private static String getRankEmoji(String rank) {
        return switch (rank) {
            case "OWNER" -> "遆";
            case "ADMIN" -> "遆";
            default -> "•";
        };
    }
}
