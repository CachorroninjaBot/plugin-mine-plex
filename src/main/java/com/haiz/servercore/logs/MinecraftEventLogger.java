package com.haiz.servercore.logs;

import com.haiz.servercore.HaizServerCore;
import com.haiz.servercore.discord.DiscordEmbedFactory;
import com.haiz.servercore.discord.DiscordLogService;
import com.haiz.servercore.metrics.MetricsManager;
import com.haiz.servercore.storage.DatabaseManager;
import com.haiz.servercore.utils.TextUtils;
import com.haiz.servercore.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Optional;

public final class MinecraftEventLogger implements Listener {
    private final HaizServerCore plugin;
    private final DatabaseManager database;
    private final MetricsManager metrics;
    private final DiscordLogService discord;

    public MinecraftEventLogger(HaizServerCore plugin, DatabaseManager database, MetricsManager metrics, DiscordLogService discord) {
        this.plugin = plugin;
        this.database = database;
        this.metrics = metrics;
        this.discord = discord;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean firstJoin = !player.hasPlayedBefore();
        metrics.recordJoin(player, firstJoin);
        if (!plugin.config().isJoinLeaveLogEnabled()) {
            return;
        }
        String playerName = player.getName();
        java.util.UUID uuid = player.getUniqueId();
        int online = Bukkit.getOnlinePlayers().size();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long now = TimeUtils.nowSeconds();
            Optional<com.haiz.servercore.metrics.PlayerStats> previous = database.players().findByUuid(uuid);
            long daysAway = previous.map(stats -> Math.max(0, (now - Math.max(stats.lastQuit(), stats.lastJoin())) / 86400)).orElse(0L);
            discord.joinLeave(DiscordEmbedFactory.join(playerName, uuid, firstJoin, online));
            if (!firstJoin && daysAway >= plugin.config().returningAfterDays()) {
                discord.alert(DiscordEmbedFactory.info("Jogador retornou",
                        playerName + " voltou apos " + daysAway + " dias fora."));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        metrics.recordQuit(player);
        if (!plugin.config().isJoinLeaveLogEnabled()) {
            return;
        }
        discord.joinLeave(DiscordEmbedFactory.leave(player.getName(), player.getUniqueId(),
                Math.max(0, Bukkit.getOnlinePlayers().size() - 1)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.config().isCommandLogEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        String sanitized = TextUtils.sanitizeCommand(event.getMessage(), plugin.config());
        metrics.recordCommand(player, sanitized);
        Location loggedLocation = player.getLocation().clone();
        String playerName = player.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> database.players().logCommand(playerName, "PLAYER", sanitized, loggedLocation, TimeUtils.nowSeconds()));
        discord.commandLog(DiscordEmbedFactory.playerCommand(playerName, primaryGroup(player), sanitized));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.config().isChatLogEnabled()) {
            return;
        }
        metrics.recordChat(event.getPlayer().getUniqueId());
        discord.serverLog(DiscordEmbedFactory.chat(event.getPlayer().getName(), TextUtils.trimForDiscord(event.getMessage(), 900)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.config().isDeathLogEnabled()) {
            return;
        }
        Player player = event.getEntity();
        metrics.recordDeath(player.getUniqueId());
        Player killer = player.getKiller();
        if (killer != null) {
            metrics.recordKill(killer.getUniqueId());
        }
        discord.serverLog(DiscordEmbedFactory.alert("Morte", event.getDeathMessage() == null ? player.getName() + " morreu." : event.getDeathMessage()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (!plugin.config().isAdvancementLogEnabled()) {
            return;
        }
        String key = event.getAdvancement().getKey().toString();
        if (key.contains("recipes/")) {
            return;
        }
        discord.serverLog(DiscordEmbedFactory.info("Advancement", event.getPlayer().getName() + " concluiu `" + key + "`."));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!plugin.config().isWorldChangeLogEnabled()) {
            return;
        }
        discord.serverLog(DiscordEmbedFactory.info("Mudanca de mundo",
                event.getPlayer().getName() + " foi de `" + event.getFrom().getName() + "` para `"
                        + event.getPlayer().getWorld().getName() + "`."));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        if (!plugin.config().isKickLogEnabled()) {
            return;
        }
        discord.alert(DiscordEmbedFactory.alert("Kick",
                event.getPlayer().getName() + " foi kickado. Motivo: " + TextUtils.trimForDiscord(event.getReason(), 500)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.config().isBlockMetricsEnabled()) {
            metrics.recordBlockBroken(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.config().isBlockMetricsEnabled()) {
            metrics.recordBlockPlaced(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (plugin.config().isMovementMetricsEnabled()) {
            metrics.touchMovement(event.getPlayer(), event.getFrom(), event.getTo());
        }
    }

    private String primaryGroup(Player player) {
        if (player.isOp()) {
            return "op";
        }
        return player.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .filter(permission -> permission.startsWith("group.") || permission.startsWith("rank."))
                .map(permission -> permission.substring(permission.indexOf('.') + 1))
                .sorted()
                .findFirst()
                .orElse("default");
    }
}
