package com.haiz.servercore.commands;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.update.UpdateManager;
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
            case "update" -> {
                if (!sender.hasPermission("haizcore.admin")) {
                    sender.sendMessage("§cSem permissão.");
                    return true;
                }
                handleUpdate(sender);
            }
            case "checkupdate" -> {
                if (!sender.hasPermission("haizcore.admin")) {
                    sender.sendMessage("§cSem permissão.");
                    return true;
                }
                handleCheckUpdate(sender);
            }
            default -> sender.sendMessage("§cComando desconhecido. Use /haizcore para ajuda.");
        }
        return true;
    }

    private void handleCheckUpdate(CommandSender sender) {
        UpdateManager updater = plugin.updateManager();
        if (updater == null) {
            sender.sendMessage("§cUpdater não está configurado.");
            return;
        }

        sender.sendMessage("§b[Updater] §fVerificando atualizações...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            updater.checkForUpdates();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (updater.isUpdateAvailable()) {
                    UpdateManager.UpdateInfo info = updater.getLatestUpdate();
                    sender.sendMessage("§a[Updater] §fNova versão disponível: §b" + info.version());
                    sender.sendMessage("§7Versão atual: §f" + updater.getCurrentVersion());
                    sender.sendMessage("§eUse §f/haizcore update §epara baixar.");
                } else {
                    sender.sendMessage("§a[Updater] §fPlugin está na versão mais recente: §b" + updater.getCurrentVersion());
                }
            });
        });
    }

    private void handleUpdate(CommandSender sender) {
        UpdateManager updater = plugin.updateManager();
        if (updater == null) {
            sender.sendMessage("§cUpdater não está configurado.");
            return;
        }

        if (updater.isUpdateDownloaded()) {
            sender.sendMessage("§e[Updater] §fUpdate já baixado. Use §6/haizcore reload §fpara aplicar.");
            return;
        }

        if (!updater.isUpdateAvailable()) {
            sender.sendMessage("§b[Updater] §fNenhuma atualização disponível.");
            return;
        }

        UpdateManager.UpdateInfo info = updater.getLatestUpdate();
        sender.sendMessage("§b[Updater] §fBaixando versão §b" + info.version() + "§f...");

        updater.downloadUpdate(sender).thenAccept(success -> {
            if (success) {
                sender.sendMessage("§a[Updater] §fDownload concluído! Use §6/haizcore reload §fpara aplicar.");
            } else {
                sender.sendMessage("§c[Updater] §fFalha ao baixar atualização.");
            }
        });
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§bHaizServerCore §7- comandos");
        sender.sendMessage("§f/haizcore status §7- status dos módulos");
        sender.sendMessage("§f/haizcore reload §7- recarrega configs");
        sender.sendMessage("§f/haizcore sendvips §7- envia embed da loja no Discord");
        sender.sendMessage("§f/haizcore checkupdate §7- verifica atualizações");
        sender.sendMessage("§f/haizcore update §7- baixa e prepara atualização");
    }

    private void status(CommandSender sender) {
        UpdateManager updater = plugin.updateManager();
        String updateStatus = "§cinativo";
        if (updater != null) {
            updateStatus = updater.isUpdateDownloaded() ? "§epronto para aplicar" :
                           updater.isUpdateAvailable() ? "§aversão disponível" : "§7atualizado";
        }

        sender.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §b§lHaizServerCore Status");
        sender.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §7Versão: §f" + plugin.getDescription().getVersion());
        sender.sendMessage("  §7Discord: §f" + plugin.discord().getStateLabel());
        sender.sendMessage("  §7VIPs: §f" + (plugin.vip() != null && plugin.vip().isRunning() ? "§aativo" : "§cinativo"));
        sender.sendMessage("  §7Update: §f" + updateStatus);
        sender.sendMessage("  §7Jogadores online: §f" + Bukkit.getOnlinePlayers().size());
        sender.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("sendvips", "reload", "status", "update", "checkupdate"));
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
