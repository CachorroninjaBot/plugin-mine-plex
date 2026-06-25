package com.haiz.servercore.teams.gui;

import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TeamMembersGUI {

    private TeamMembersGUI() {}

    public static void open(TeamsModule module, Player player, Object team) {
        TeamsBridge bridge = module.bridge();
        List<Object> members = bridge.getMembers(team);
        UUID playerUUID = player.getUniqueId();
        boolean isOwner = bridge.isOwner(team, playerUUID);
        boolean isAdmin = bridge.isAdmin(team, playerUUID);

        int rows = Math.max(3, (int) Math.ceil((members.size() + 2) / 9.0) + 1);
        rows = Math.min(rows, 6);
        Inventory inv = Bukkit.createInventory(new MembersHolder(team), rows * 9, "§8§lMembros do Time");

        for (int i = 0; i < members.size() && i < (rows - 1) * 9; i++) {
            Object member = members.get(i);
            UUID memberUUID = bridge.getPlayerUUID(member);
            String rank = bridge.getPlayerRank(member);
            String rankPrefix = switch (rank) {
                case "OWNER" -> "§6遆 ";
                case "ADMIN" -> "§e遆 ";
                default -> "§7";
            };

            OfflinePlayer op = Bukkit.getOfflinePlayer(memberUUID);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(op);
                meta.setDisplayName(rankPrefix + bridge.resolvePlayerName(memberUUID));

                List<String> lore = new ArrayList<>();
                lore.add("§7Rank: §f" + rank);
                String title = bridge.getPlayerTitle(member);
                if (title != null && !title.isEmpty()) {
                    lore.add("§7Título: §f" + title);
                }
                lore.add("§7Status: " + (Bukkit.getPlayer(memberUUID) != null ? "§aOnline" : "§cOffline"));
                lore.add("");

                if (!memberUUID.equals(playerUUID)) {
                    if (isOwner && !"OWNER".equals(rank)) {
                        if ("DEFAULT".equals(rank)) {
                            lore.add("§a§lSHIFT-ESQUERDO §7para promover a Admin");
                        } else if ("ADMIN".equals(rank)) {
                            lore.add("§e§lSHIFT-ESQUERDO §7para promover a Dono");
                        }
                        lore.add("§c§lSHIFT-DIREITO §7para rebaixar");
                        lore.add("§4§lMIDDLE-CLICK §7para expulsar");
                    } else if (isAdmin && "DEFAULT".equals(rank)) {
                        lore.add("§a§lSHIFT-ESQUERDO §7para promover");
                        lore.add("§4§lSHIFT-DIREITO §7para expulsar");
                    }
                } else {
                    lore.add("§7§o(Você)");
                }

                meta.setLore(lore);
                skull.setItemMeta(meta);
            }
            inv.setItem(i, skull);
        }

        inv.setItem((rows * 9) - 1, TeamMainMenu.createItem(Material.ARROW, "§a§lVoltar", "§7Voltar ao menu principal"));
        inv.setItem((rows * 9) - 5, TeamMainMenu.createItem(Material.BARRIER, "§c§lFechar", "§7Fechar menu"));

        player.openInventory(inv);
    }

    private static class MembersHolder implements TeamGUIHolder {
        private final Object team;

        MembersHolder(Object team) {
            this.team = team;
        }

        @Override
        public Inventory getInventory() { return null; }

        @Override
        public void handleClick(TeamsModule module, Player player, int slot, ClickType clickType) {
            TeamsBridge bridge = module.bridge();
            List<Object> members = bridge.getMembers(team);
            UUID playerUUID = player.getUniqueId();
            boolean isOwner = bridge.isOwner(team, playerUUID);
            boolean isAdmin = bridge.isAdmin(team, playerUUID);

            int lastRowStart = (player.getOpenInventory().getTopInventory().getSize() / 9 - 1) * 9;
            if (slot == lastRowStart + 8) {
                TeamMainMenu.open(module, player);
                return;
            }
            if (slot == lastRowStart + 4) {
                player.closeInventory();
                return;
            }

            if (slot >= members.size()) return;

            Object targetMember = members.get(slot);
            UUID targetUUID = bridge.getPlayerUUID(targetMember);
            String targetRank = bridge.getPlayerRank(targetMember);

            if (targetUUID.equals(playerUUID)) return;

            if (clickType == ClickType.SHIFT_LEFT) {
                if (isOwner && !"OWNER".equals(targetRank)) {
                    bridge.promotePlayer(team, targetUUID);
                    player.sendMessage("§aJogador promovido!");
                    open(module, player, team);
                } else if (isAdmin && "DEFAULT".equals(targetRank)) {
                    bridge.promotePlayer(team, targetUUID);
                    player.sendMessage("§aJogador promovido a Admin!");
                    open(module, player, team);
                }
            } else if (clickType == ClickType.SHIFT_RIGHT) {
                if (isOwner && !"OWNER".equals(targetRank)) {
                    bridge.demotePlayer(team, targetUUID);
                    player.sendMessage("§cJogador rebaixado!");
                    open(module, player, team);
                } else if (isAdmin && "DEFAULT".equals(targetRank)) {
                    bridge.kickPlayer(team, targetUUID);
                    player.sendMessage("§cJogador expulso!");
                    open(module, player, team);
                }
            } else if (clickType == ClickType.MIDDLE) {
                if (isOwner && !"OWNER".equals(targetRank)) {
                    bridge.kickPlayer(team, targetUUID);
                    player.sendMessage("§4Jogador expulso do time!");
                    open(module, player, team);
                }
            }
        }
    }
}
