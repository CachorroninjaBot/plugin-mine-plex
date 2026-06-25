package com.haiz.servercore.teams.gui;

import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public final class TeamMainMenu {

    private TeamMainMenu() {}

    public static void open(TeamsModule module, Player player) {
        TeamsBridge bridge = module.bridge();
        Object team = bridge.getTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage("§cVocê não pertence a nenhum time.");
            return;
        }

        String title = module.teamsConfig().guiTitle();
        Inventory inv = Bukkit.createInventory(new MainMenuHolder(team), 54, title);

        UUID playerUUID = player.getUniqueId();
        boolean isOwner = bridge.isOwner(team, playerUUID);
        boolean isAdmin = bridge.isAdmin(team, playerUUID);

        fillGlass(inv);

        inv.setItem(10, createItem(Material.PLAYER_HEAD, "§e§lMembros",
                "§7Clique para gerenciar membros",
                "§7Membros: §f" + bridge.getMembers(team).size()));

        inv.setItem(11, createItem(Material.ANVIL, "§e§lConfigurações",
                isOwner ? "§7Clique para alterar configurações" : "§cApenas o dono pode alterar"));

        inv.setItem(12, createItem(Material.COMPASS, "§e§lHome",
                bridge.getTeamHome(team) != null ? "§7Clique para teleportar" : "§7Nenhuma home definida"));

        inv.setItem(13, createItem(Material.ENDER_PEARL, "§e§lWarps",
                "§7Clique para ver warps",
                "§7Warps: §f" + bridge.getWarps(team).size()));

        inv.setItem(14, createItem(Material.SHIELD, "§e§lAlianças",
                "§7Clique para ver alianças",
                "§7Aliados: §f" + bridge.getAllies(team).size()));

        inv.setItem(15, createItem(Material.GOLD_INGOT, "§e§lBanco",
                "§7Saldo: §a$" + String.format("%.2f", bridge.getTeamMoney(team)),
                "§7Clique para depositar/sacar"));

        inv.setItem(16, createItem(Material.EXPERIENCE_BOTTLE, "§e§lLevel",
                "§7Level: §a" + bridge.getTeamLevel(team),
                "§7Score: §a" + String.format("%.0f", bridge.getTeamScore(team))));

        inv.setItem(22, createItem(Material.NAME_TAG, "§e§lTag",
                "§7Tag atual: §f" + bridge.getTeamTag(team),
                isOwner ? "§7Clique para alterar" : "§cApenas o dono"));

        inv.setItem(31, createItem(Material.LIME_DYE, "§a§lPvP",
                bridge.isTeamPvp(team) ? "§aAtivado" : "§cDesativado",
                isOwner ? "§7Clique para alternar" : "§cApenas o dono"));

        inv.setItem(49, createItem(Material.BARRIER, "§c§lFechar", "§7Clique para fechar o menu"));

        player.openInventory(inv);
    }

    private static void fillGlass(Inventory inv) {
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }

    static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(List.of(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static class MainMenuHolder implements TeamGUIHolder {
        private final Object team;

        MainMenuHolder(Object team) {
            this.team = team;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }

        @Override
        public void handleClick(TeamsModule module, Player player, int slot, ClickType clickType) {
            TeamsBridge bridge = module.bridge();
            UUID playerUUID = player.getUniqueId();
            boolean isOwner = bridge.isOwner(team, playerUUID);
            boolean isAdmin = bridge.isAdmin(team, playerUUID);

            switch (slot) {
                case 10 -> TeamMembersGUI.open(module, player, team);
                case 11 -> {
                    if (isOwner) TeamSettingsGUI.open(module, player, team);
                    else player.sendMessage("§cApenas o dono pode alterar configurações.");
                }
                case 12 -> {
                    var home = bridge.getTeamHome(team);
                    if (home != null) {
                        player.teleport(home);
                        player.sendMessage("§aTeletransportado para a home do time!");
                    } else {
                        player.sendMessage("§cNenhuma home definida.");
                    }
                }
                case 13 -> TeamWarpsGUI.open(module, player, team);
                case 14 -> TeamAlliesGUI.open(module, player, team);
                case 15 -> TeamBankGUI.open(module, player, team);
                case 16 -> TeamLevelGUI.open(module, player, team);
                case 22 -> {
                    if (isOwner) {
                        player.sendMessage("§eUse §f/team tag <tag> §epara alterar a tag.");
                    } else {
                        player.sendMessage("§cApenas o dono pode alterar a tag.");
                    }
                }
                case 31 -> {
                    if (isOwner) {
                        boolean newPvp = !bridge.isTeamPvp(team);
                        bridge.setTeamPvp(team, newPvp);
                        player.sendMessage(newPvp ? "§aPvP entre membros ativado!" : "§cPvP entre membros desativado!");
                        open(module, player);
                    } else {
                        player.sendMessage("§cApenas o dono pode alterar o PvP.");
                    }
                }
                case 49 -> player.closeInventory();
            }
        }
    }
}
