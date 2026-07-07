package com.haiz.servercore.commands;

import com.haiz.servercore.HaizServerCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HaizCoreCommand implements CommandExecutor, TabCompleter {
    private final HaizServerCore plugin;

    public HaizCoreCommand(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("haizcore.admin")) {
                    sender.sendMessage("§cSem permissão.");
                    return true;
                }
                plugin.reloadEverything();
                sender.sendMessage("§aConfigurações recarregadas.");
            }
            case "status" -> {
                if (!sender.hasPermission("haizcore.admin")) {
                    sender.sendMessage("§cSem permissão.");
                    return true;
                }
                status(sender);
            }
            case "sendvips" -> {
                if (!sender.hasPermission("haizcore.vip.send")) {
                    sender.sendMessage("§cSem permissão.");
                    return true;
                }
                plugin.vip().sendShopMessage(sender);
            }
            default -> sender.sendMessage("§cComando desconhecido. Use /haizcore para ajuda.");
        }
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§bHaizServerCore §7- comandos");
        sender.sendMessage("§f/haizcore status §7- status dos módulos");
        sender.sendMessage("§f/haizcore reload §7- recarrega configs");
        sender.sendMessage("§f/haizcore sendvips §7- envia embed da loja no Discord");
    }

    private void status(CommandSender sender) {
        sender.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §b§lHaizServerCore Status");
        sender.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §7Versão: §f" + plugin.getDescription().getVersion());
        sender.sendMessage("  §7Discord: §f" + plugin.discord().getStateLabel());
        sender.sendMessage("  §7VIPs: §f" + (plugin.vip() != null && plugin.vip().isRunning() ? "§aativo" : "§cinativo"));
        sender.sendMessage("  §7Teams: §f" + (plugin.teams() != null && plugin.teams().isRunning() ? "§aativo" : "§cinativo"));
        sender.sendMessage("  §7Jogadores online: §f" + Bukkit.getOnlinePlayers().size());
        sender.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("sendvips", "reload", "status"));
        }
        return List.of();
    }

    private List<String> filter(String prefix, List<String> values) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }
}
