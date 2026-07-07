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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TeamAlliesGUI {

    private TeamAlliesGUI() {}

    public static void open(TeamsModule module, Player player, Object team) {
        TeamsBridge bridge = module.bridge();
        Set<UUID> allies = bridge.getAllyIds(team);
        boolean isOwner = bridge.isOwner(team, player.getUniqueId());

        int rows = Math.max(3, (int) Math.ceil((allies.size() + 2) / 9.0) + 1);
        rows = Math.min(rows, 6);
        Inventory inv = Bukkit.createInventory(new AlliesHolder(team), rows * 9, "§8§lAlianças do Time");

        int i = 0;
        for (UUID allyId : allies) {
            ItemStack item = new ItemStack(Material.SHIELD);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b§l" + bridge.getAllyName(allyId));
                List<String> lore = new ArrayList<>();
                lore.add("§7ID: §f" + allyId.toString().substring(0, 8) + "...");
                if (isOwner) {
                    lore.add("");
                    lore.add("§c§lSHIFT-DIREITO §7para remover aliança");
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

    private static class AlliesHolder implements TeamGUIHolder {
        private final Object team;

        AlliesHolder(Object team) { this.team = team; }

        @Override
        public Inventory getInventory() { return null; }

        @Override
        public void handleClick(TeamsModule module, Player player, int slot, ClickType clickType) {
            TeamsBridge bridge = module.bridge();
            List<UUID> allies = new ArrayList<>(bridge.getAllyIds(team));

            int lastRowStart = (player.getOpenInventory().getTopInventory().getSize() / 9 - 1) * 9;
            if (slot == lastRowStart + 8) {
                TeamMainMenu.open(module, player);
                return;
            }
            if (slot == lastRowStart + 4) {
                player.closeInventory();
                return;
            }
            if (slot >= allies.size()) return;

            UUID allyId = allies.get(slot);

            if (clickType == ClickType.SHIFT_RIGHT && bridge.isOwner(team, player.getUniqueId())) {
                bridge.removeAlly(team, allyId);
                player.sendMessage("§cAliança removida!");
                open(module, player, team);
            }
        }
    }
}
