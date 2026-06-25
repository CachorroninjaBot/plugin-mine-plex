package com.haiz.servercore.teams;

import com.haiz.servercore.teams.gui.TeamMainMenu;
import com.haiz.servercore.teams.web.WebAuthHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TeamMenuCommand implements CommandExecutor, TabCompleter {

    private final TeamsModule module;

    public TeamMenuCommand(TeamsModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }
        if (!module.isRunning()) {
            player.sendMessage("§cMódulo de times não está ativo.");
            return true;
        }

        if (args.length >= 2 && "web".equalsIgnoreCase(args[0])) {
            String code = args[1].toUpperCase();
            if (WebAuthHandler.verifyCode(code, player.getUniqueId())) {
                player.sendMessage("§a§lCódigo verificado! §7Volte ao navegador para continuar.");
            } else {
                player.sendMessage("§cCódigo inválido ou expirado.");
            }
            return true;
        }

        TeamMainMenu.open(module, player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("web");
        }
        return Collections.emptyList();
    }
}
