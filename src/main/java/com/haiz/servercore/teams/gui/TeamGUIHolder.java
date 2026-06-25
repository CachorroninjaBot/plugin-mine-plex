package com.haiz.servercore.teams.gui;

import com.haiz.servercore.teams.TeamsModule;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface TeamGUIHolder extends InventoryHolder {
    void handleClick(TeamsModule module, Player player, int slot, ClickType clickType);
}
