package com.haiz.servercore.vip;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RepairCommand implements CommandExecutor, TabCompleter {

    private final AutoRepairManager autoRepairManager;

    public RepairCommand(AutoRepairManager autoRepairManager) {
        this.autoRepairManager = autoRepairManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando é apenas para jogadores.");
            return true;
        }

        if (autoRepairManager == null || !autoRepairManager.isEnabled()) {
            player.sendMessage("§cAuto-repair está desativado.");
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "hand";

        switch (sub) {
            case "hand", "mao", "mão" -> autoRepairManager.repairHand(player);
            case "all", "todos", "inv", "inventario" -> autoRepairManager.repairAll(player);
            default -> player.sendMessage("§8[§bHaizCore§8] §cUso: §f/repair [hand|all]");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("hand", "all").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return Collections.emptyList();
    }
}
