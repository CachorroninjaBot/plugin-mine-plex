package com.haiz.servercore.vip;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /linkar <código>  – jogador confirma vínculo Discord no Minecraft
 *
 * Registrado em plugin.yml como "linkar".
 */
public final class VipMinecraftCommands implements CommandExecutor, TabCompleter {

    private final VipModule module;

    public VipMinecraftCommands(VipModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando é apenas para jogadores.");
            return true;
        }

        String name = command.getName().toLowerCase(Locale.ROOT);

        if (name.equals("linkar")) {
            handleLinkar(player, args);
            return true;
        }

        return false;
    }

    private void handleLinkar(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUso: /linkar <código>");
            return;
        }

        String code = args[0].toUpperCase(Locale.ROOT);
        LinkManager.ConfirmResult result = module.linkManager().confirmCode(player, code);

        switch (result) {
            case SUCCESS -> {
                player.sendMessage("");
                player.sendMessage("§8[§bHaizCore§8] §a§lConta vinculada com sucesso!");
                player.sendMessage("§7Sua conta Discord foi vinculada a §b" + player.getName() + "§7.");
                player.sendMessage("§7Agora você pode comprar VIPs pelo Discord.");
                player.sendMessage("");
            }
            case INVALID_CODE -> player.sendMessage("§8[§bHaizCore§8] §cCódigo inválido ou não pertence a você.");
            case EXPIRED    -> player.sendMessage("§8[§bHaizCore§8] §cEste código expirou. Solicite um novo no Discord.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList(); // Não sugerir códigos no tab
    }
}