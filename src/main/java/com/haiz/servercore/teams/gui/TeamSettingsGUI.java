package com.haiz.servercore.teams.gui;

import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

public final class TeamSettingsGUI {

    private TeamSettingsGUI() {}

    public static void open(TeamsModule module, Player player, Object team) {
        TeamsBridge bridge = module.bridge();
        Inventory inv = Bukkit.createInventory(new SettingsHolder(team), 36, "§8§lConfigurações do Time");

        inv.setItem(10, TeamMainMenu.createItem(Material.NAME_TAG, "§e§lNome",
                "§7Nome atual: §f" + bridge.getTeamName(team),
                "§7Clique para alterar"));

        inv.setItem(11, TeamMainMenu.createItem(Material.PAPER, "§e§lTag",
                "§7Tag atual: §f" + bridge.getTeamTag(team),
                "§7Clique para alterar"));

        inv.setItem(12, TeamMainMenu.createItem(Material.BOOK, "§e§lDescrição",
                "§7Atual: §f" + bridge.getTeamDescription(team),
                "§7Clique para alterar"));

        inv.setItem(13, TeamMainMenu.createItem(Material.LIME_DYE, bridge.isTeamOpen(team) ? "§a§lAberto" : "§c§lFechado",
                "§7Times abertos aceitam jogadores sem convite",
                "§7Clique para alternar"));

        inv.setItem(14, TeamMainMenu.createItem(Material.IRON_SWORD, bridge.isTeamPvp(team) ? "§a§lPvP Ativo" : "§c§lPvP Inativo",
                "§7PvP entre membros do time",
                "§7Clique para alternar"));

        inv.setItem(15, TeamMainMenu.createItem(Material.WHITE_WOOL, "§e§lCor",
                "§7Cor atual: §f" + bridge.getTeamColor(team),
                "§7Clique para alterar"));

        inv.setItem(22, TeamMainMenu.createItem(Material.LAVA_BUCKET, "§4§lDisbandar Time",
                "§c§lATENÇÃO: §7Esta ação é irreversível!",
                "§7Clique para disbandar o time"));

        inv.setItem(31, TeamMainMenu.createItem(Material.BARRIER, "§c§lFechar"));
        inv.setItem(30, TeamMainMenu.createItem(Material.ARROW, "§a§lVoltar"));

        player.openInventory(inv);
    }

    private static class SettingsHolder implements TeamGUIHolder {
        private final Object team;

        SettingsHolder(Object team) { this.team = team; }

        @Override
        public Inventory getInventory() { return null; }

        @Override
        public void handleClick(TeamsModule module, Player player, int slot, ClickType clickType) {
            TeamsBridge bridge = module.bridge();

            switch (slot) {
                case 10 -> player.sendMessage("§eUse §f/team name <novo_nome> §epara alterar o nome.");
                case 11 -> player.sendMessage("§eUse §f/team tag <nova_tag> §epara alterar a tag.");
                case 12 -> player.sendMessage("§eUse §f/team description <nova_desc> §epara alterar a descrição.");
                case 13 -> {
                    boolean newOpen = !bridge.isTeamOpen(team);
                    bridge.setTeamOpen(team, newOpen);
                    player.sendMessage(newOpen ? "§aTime definido como aberto!" : "§cTime definido como fechado!");
                    open(module, player, team);
                }
                case 14 -> {
                    boolean newPvp = !bridge.isTeamPvp(team);
                    bridge.setTeamPvp(team, newPvp);
                    player.sendMessage(newPvp ? "§aPvP ativado!" : "§cPvP desativado!");
                    open(module, player, team);
                }
                case 15 -> player.sendMessage("§eUse §f/team color <cor> §epara alterar a cor.");
                case 22 -> {
                    player.closeInventory();
                    player.sendMessage("§4§lATENÇÃO: §cPara disbandar o time, use §f/team disband§c.");
                }
                case 30 -> TeamMainMenu.open(module, player);
                case 31 -> player.closeInventory();
            }
        }
    }
}
