package com.haiz.servercore.commands;

import com.haiz.servercore.HaizServerCore;
import org.bukkit.command.CommandSender;

public final class ReloadCommand {
    private final HaizServerCore plugin;

    public ReloadCommand(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender) {
        plugin.reloadEverything();
        sender.sendMessage(plugin.messages().get("reload-success"));
    }
}
