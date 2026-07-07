package com.haiz.servercore.teams.gui;

import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

public final class TeamLevelGUI {

    private TeamLevelGUI() {}

    public static void open(TeamsModule module, Player player, Object team) {
        TeamsBridge bridge = module.bridge();
        boolean isOwner = bridge.isOwner(team, player.getUniqueId());

        Inventory inv = Bukkit.createInventory(new LevelHolder(team), 36, "§8§lLevel do Time");

        int level = bridge.getTeamLevel(team);
        double score = bridge.getTeamScore(team);

        inv.setItem(11, TeamMainMenu.createItem(Material.EXPERIENCE_BOTTLE, "§b§lLevel Atual",
                "§7Level: §a" + level,
                "§7Score: §a" + score));

        inv.setItem(13, TeamMainMenu.createItem(Material.DIAMOND, "§e§lInformações",
                "§7Limite de membros: §a" + getLevelLimit(level),
                "§7Max warps: §a" + getLevelWarps(level),
                "§7Max chest claims: §a" + getLevelChests(level)));

        if (isOwner) {
            inv.setItem(15, TeamMainMenu.createItem(Material.NETHER_STAR, "§a§lRank Up",
                    "§7Clique para upar o level do time",
                    "§7Requer score ou dinheiro suficiente"));
        }

        inv.setItem(31, TeamMainMenu.createItem(Material.BARRIER, "§c§lFechar"));
        inv.setItem(30, TeamMainMenu.createItem(Material.ARROW, "§a§lVoltar"));

        player.openInventory(inv);
    }

    private static String getLevelLimit(int level) {
        return switch (level) {
            case 1 -> "10";
            case 2 -> "20";
            case 3 -> "30";
            case 4 -> "50";
            default -> "Ilimitado";
        };
    }

    private static String getLevelWarps(int level) {
        return switch (level) {
            case 1 -> "3";
            case 2 -> "5";
            case 3 -> "10";
            default -> "Ilimitado";
        };
    }

    private static String getLevelChests(int level) {
        return switch (level) {
            case 1 -> "2";
            case 2 -> "5";
            case 3 -> "10";
            default -> "Ilimitado";
        };
    }

    private static class LevelHolder implements TeamGUIHolder {
        private final Object team;

        LevelHolder(Object team) { this.team = team; }

        @Override
        public Inventory getInventory() { return null; }

        @Override
        public void handleClick(TeamsModule module, Player player, int slot, ClickType clickType) {
            TeamsBridge bridge = module.bridge();

            switch (slot) {
                case 15 -> {
                    if (bridge.isOwner(team, player.getUniqueId())) {
                        if (bridge.levelupTeam(team)) {
                            player.sendMessage("§aTime upado para o level §f" + bridge.getTeamLevel(team) + "§a!");
                        } else {
                            player.sendMessage("§cNão foi possível fazer rankup. Verifique score/dinheiro.");
                        }
                        open(module, player, team);
                    }
                }
                case 30 -> TeamMainMenu.open(module, player);
                case 31 -> player.closeInventory();
            }
        }
    }
}
