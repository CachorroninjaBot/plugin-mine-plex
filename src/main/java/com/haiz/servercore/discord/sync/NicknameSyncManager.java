package com.haiz.servercore.discord.sync;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.config.ConfigManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class NicknameSyncManager {
    private final HaizServerCore plugin;
    private int taskId = -1;

    public NicknameSyncManager(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        ConfigManager config = plugin.config();
        if (!config.isNicknameSyncEnabled()) return;
        if (!plugin.discord().isOnline()) return;

        int cycleMinutes = config.nicknameSyncCycleMinutes();
        if (cycleMinutes > 0) {
            taskId = new BukkitRunnable() {
                @Override
                public void run() {
                    syncAllOnlinePlayers();
                }
            }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60 * cycleMinutes).getTaskId();
        }

        plugin.getLogger().info("[Sync] Nickname sync iniciado. Cycle=" + cycleMinutes + "min");
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void syncAllOnlinePlayers() {
        if (!plugin.discord().isOnline()) return;

        Guild guild = plugin.discord().guild();
        if (guild == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            syncPlayer(player, guild);
        }
    }

    public void syncPlayer(Player player, Guild guild) {
        ConfigManager config = plugin.config();
        if (!config.isNicknameSyncEnabled()) return;

        String discordId = getDiscordId(player.getUniqueId().toString());
        if (discordId == null || discordId.isBlank()) return;

        Member member = guild.getMemberById(discordId);
        if (member == null) return;

        String format = config.nicknameSyncFormat();
        String nickname = format
                .replace("%displayname%", player.getDisplayName())
                .replace("%username%", player.getName())
                .replace("%discord_name%", member.getEffectiveName());

        if (nickname.length() > 32) {
            nickname = nickname.substring(0, 32);
        }

        try {
            if (!member.getEffectiveName().equals(nickname)) {
                member.modifyNickname(nickname).queue();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Sync] Falha ao sincronizar apelido: " + e.getMessage());
        }
    }

    private String getDiscordId(String uuid) {
        try {
            return plugin.sqliteDatabase().getDiscordIdByUuid(uuid);
        } catch (Exception e) {
            return null;
        }
    }
}
