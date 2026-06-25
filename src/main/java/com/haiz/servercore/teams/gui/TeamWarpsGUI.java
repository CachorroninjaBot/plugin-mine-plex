package com.haiz.servercore.teams.gui;

import com.haiz.servercore.teams.TeamsBridge;
import com.haiz.servercore.teams.TeamsModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TeamWarpsGUI {

    private TeamWarpsGUI() {}

    public static void open(TeamsModule module, Player player, Object team) {
        TeamsBridge bridge = module.bridge();
        Map<String, Object> warps = bridge.getWarps(team);
        boolean isOwner = bridge.isOwner(team, player.getUniqueId());
        boolean isAdmin = bridge.isAdmin(team, player.getUniqueId());

        int rows = Math.max(3, (int) Math.ceil((warps.size() + 2) / 9.0) + 1);
        rows = Math.min(rows, 6);
        Inventory inv = Bukkit.createInventory(new WarpsHolder(team), rows * 9, "§8§lWarps do Time");

        int i = 0;
        for (Map.Entry<String, Object> entry : warps.entrySet()) {
            Location loc = bridge.getWarpLocation(entry.getValue());
            ItemStack item = new ItemStack(Material.ENDER_PEARL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e§l" + entry.getKey());
                List<String> lore = new ArrayList<>();
                if (loc != null && loc.getWorld() != null) {
                    lore.add("§7Mundo: §f" + loc.getWorld().getName());
                    lore.add("§7X: §f" + String.format("%.1f", loc.getX()));
                    lore.add("§7Y: §f" + String.format("%.1f", loc.getY()));
                    lore.add("§7Z: §f" + String.format("%.1f", loc.getZ()));
                }
                lore.add("");
                lore.add("§a§lESQUERDO §7para teleportar");
                if (isOwner || isAdmin) {
                    lore.add("§c§lDIREITO §7para remover");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(i++, item);
        }

        int lastRowStart = (rows - 1) * 9;
        inv.setItem(lastRowStart + 8, TeamMainMenu.createItem(Material.ARROW, "§a§lVoltar"));
        inv.setItem(lastRowStart + 4, TeamMainMenu.createItem(Material.BARRIER, "§c§lFechar"));

        player.openInventory(inv);
    }

    private static class WarpsHolder implements TeamGUIHolder {
        private final Object team;

        WarpsHolder(Object team) { this.team = team; }

        @Override
        public Inventory getInventory() { return null; }

        @Override
        public void handleClick(TeamsModule module, Player player, int slot, ClickType clickType) {
            TeamsBridge bridge = module.bridge();
            Map<String, Object> warps = bridge.getWarps(team);
            List<String> warpNames = new ArrayList<>(warps.keySet());

            int lastRowStart = (player.getOpenInventory().getTopInventory().getSize() / 9 - 1) * 9;
            if (slot == lastRowStart + 8) {
                TeamMainMenu.open(module, player);
                return;
            }
            if (slot == lastRowStart + 4) {
                player.closeInventory();
                return;
            }
            if (slot >= warpNames.size()) return;

            String warpName = warpNames.get(slot);

            if (clickType == ClickType.LEFT) {
                Object warp = warps.get(warpName);
                Location loc = bridge.getWarpLocation(warp);
                if (loc != null) {
                    player.teleport(loc);
                    player.sendMessage("§aTeletransportado para o warp §f" + warpName + "§a!");
                }
            } else if (clickType == ClickType.RIGHT) {
                if (bridge.isOwner(team, player.getUniqueId()) || bridge.isAdmin(team, player.getUniqueId())) {
                    bridge.deleteWarp(team, warpName);
                    player.sendMessage("§cWarp §f" + warpName + " §cremovido!");
                    open(module, player, team);
                }
            }
        }
    }
}
