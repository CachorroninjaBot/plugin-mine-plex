package com.haiz.servercore.teams.gui;

import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

public final class TeamBankGUI {

    private TeamBankGUI() {}

    public static void open(TeamsModule module, Player player, Object team) {
        TeamsBridge bridge = module.bridge();
        boolean isOwner = bridge.isOwner(team, player.getUniqueId());
        boolean isAdmin = bridge.isAdmin(team, player.getUniqueId());

        Inventory inv = Bukkit.createInventory(new BankHolder(team), 36, "§8§lBanco do Time");

        inv.setItem(11, TeamMainMenu.createItem(Material.GOLD_BLOCK, "§6§lSaldo",
                "§7Saldo atual: §a$" + String.format("%.2f", bridge.getTeamMoney(team))));

        inv.setItem(13, TeamMainMenu.createItem(Material.EXPERIENCE_BOTTLE, "§b§lScore",
                "§7Score do time: §a" + String.format("%.0f", bridge.getTeamScore(team)),
                "§7Level: §a" + bridge.getTeamLevel(team)));

        inv.setItem(15, TeamMainMenu.createItem(Material.EMERALD, "§a§lDepositar",
                "§7Clique para depositar $1000",
                "§7SHIFT para depositar $10000"));

        if (isOwner || isAdmin) {
            inv.setItem(23, TeamMainMenu.createItem(Material.REDSTONE, "§c§lSacar",
                    "§7Clique para sacar $1000",
                    "§7SHIFT para sacar $10000"));
        }

        inv.setItem(31, TeamMainMenu.createItem(Material.BARRIER, "§c§lFechar"));
        inv.setItem(30, TeamMainMenu.createItem(Material.ARROW, "§a§lVoltar"));

        player.openInventory(inv);
    }

    private static class BankHolder implements TeamGUIHolder {
        private final Object team;

        BankHolder(Object team) { this.team = team; }

        @Override
        public Inventory getInventory() { return null; }

        @Override
        public void handleClick(TeamsModule module, Player player, int slot, ClickType clickType) {
            TeamsBridge bridge = module.bridge();

            switch (slot) {
                case 15 -> {
                    double amount = clickType.isShiftClick() ? 10000 : 1000;
                    if (bridge.depositMoney(team, amount)) {
                        player.sendMessage("§aDepositado §f$" + String.format("%.2f", amount) + " §ano banco do time!");
                    } else {
                        player.sendMessage("§cSaldo insuficiente.");
                    }
                    open(module, player, team);
                }
                case 23 -> {
                    if (!bridge.isOwner(team, player.getUniqueId()) && !bridge.isAdmin(team, player.getUniqueId())) {
                        player.sendMessage("§cSem permissão para sacar.");
                        return;
                    }
                    double amount = clickType.isShiftClick() ? 10000 : 1000;
                    if (bridge.withdrawMoney(team, amount)) {
                        player.sendMessage("§cSacado §f$" + String.format("%.2f", amount) + " §cdo banco do time!");
                    } else {
                        player.sendMessage("§cSaldo insuficiente no banco.");
                    }
                    open(module, player, team);
                }
                case 30 -> TeamMainMenu.open(module, player);
                case 31 -> player.closeInventory();
            }
        }
    }
}
