package com.haiz.servercore.discord.sync;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.config.ConfigManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public final class GroupRoleSyncManager {
    private final HaizServerCore plugin;
    private int taskId = -1;

    public GroupRoleSyncManager(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        ConfigManager config = plugin.config();
        if (!config.isGroupRoleSyncEnabled()) return;
        if (!plugin.discord().isOnline()) return;

        int cycleMinutes = config.groupRoleSyncCycleMinutes();
        if (cycleMinutes > 0) {
            taskId = new BukkitRunnable() {
                @Override
                public void run() {
                    syncAllOnlinePlayers();
                }
            }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60 * cycleMinutes).getTaskId();
        }

        plugin.getLogger().info("[Sync] Group/Role sync iniciado. Cycle=" + cycleMinutes + "min");
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
        if (!config.isGroupRoleSyncEnabled()) return;

        Member member = guild.getMemberById(getDiscordId(player.getUniqueId().toString()));
        if (member == null) return;

        Map<String, String> mappings = getGroupRoleMappings();
        if (mappings.isEmpty()) return;

        boolean minecraftAuthoritative = config.isGroupRoleSyncMinecraftAuthoritative();

        if (minecraftAuthoritative) {
            syncMinecraftToDiscord(player, member, mappings);
        } else {
            syncDiscordToMinecraft(player, member, mappings);
        }
    }

    private void syncMinecraftToDiscord(Player player, Member member, Map<String, String> mappings) {
        String primaryGroup = getPlayerPrimaryGroup(player);

        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String mcGroup = entry.getKey();
            String discordRoleId = entry.getValue();

            Role role = member.getGuild().getRoleById(discordRoleId);
            if (role == null) continue;

            boolean hasGroup = primaryGroup.equalsIgnoreCase(mcGroup);
            boolean hasRole = member.getRoles().contains(role);

            try {
                if (hasGroup && !hasRole) {
                    member.getGuild().addRoleToMember(member, role).queue();
                } else if (!hasGroup && hasRole) {
                    member.getGuild().removeRoleFromMember(member, role).queue();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Sync] Falha ao sincronizar grupo " + mcGroup + ": " + e.getMessage());
            }
        }
    }

    private void syncDiscordToMinecraft(Player player, Member member, Map<String, String> mappings) {
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String mcGroup = entry.getKey();
            String discordRoleId = entry.getValue();

            Role role = member.getGuild().getRoleById(discordRoleId);
            if (role == null) continue;

            boolean hasRole = member.getRoles().contains(role);

            try {
                if (hasRole) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "lp user " + player.getName() + " parent add " + mcGroup);
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "lp user " + player.getName() + " parent remove " + mcGroup);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Sync] Falha ao sincronizar cargo " + mcGroup + ": " + e.getMessage());
            }
        }
    }

    private Map<String, String> getGroupRoleMappings() {
        Map<String, String> mappings = new HashMap<>();
        try {
            var config = plugin.getConfig().getConfigurationSection("synchronization.group-role.mappings");
            if (config != null) {
                for (String key : config.getKeys(false)) {
                    String value = config.getString(key);
                    if (value != null && !value.isBlank()) {
                        mappings.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Sync] Falha ao carregar mapeamentos: " + e.getMessage());
        }
        return mappings;
    }

    private String getPlayerPrimaryGroup(Player player) {
        try {
            net.milkbowl.vault.chat.Chat chat = org.bukkit.Bukkit.getServicesManager()
                    .getRegistration(net.milkbowl.vault.chat.Chat.class) != null
                    ? org.bukkit.Bukkit.getServicesManager()
                    .getRegistration(net.milkbowl.vault.chat.Chat.class).getProvider()
                    : null;
            if (chat != null) {
                return chat.getPrimaryGroup(null, player);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Sync] Falha ao obter grupo Vault: " + e.getMessage());
        }
        return "player";
    }

    private String getDiscordId(String uuid) {
        try {
            return plugin.sqliteDatabase().getDiscordIdByUuid(uuid);
        } catch (Exception e) {
            return null;
        }
    }
}
