package com.haiz.servercore.vip;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class VipSettingsCommand implements CommandExecutor, TabCompleter {

    private final VipModule module;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public VipSettingsCommand(VipModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando é apenas para jogadores.");
            return true;
        }

        if (args.length == 0) {
            showStatus(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> showStatus(player);
            case "autoresnovar", "autores", "auto", "renovar" -> toggleAutoRenew(player);
            case "autoreparo", "repair", "reparo" -> toggleAutoRepair(player);
            default -> player.sendMessage("§8[§bHaizCore§8] §cSubcomandos: §fstatus, autoresnovar, autoreparo");
        }
        return true;
    }

    private void showStatus(Player player) {
        UUID uuid = player.getUniqueId();
        VipStorage storage = module.vipStorage();

        CompletableFuture.supplyAsync(() -> storage.getActiveSubscription(uuid))
                .thenAccept(sub -> {
                    Runnable send = () -> {
                        player.sendMessage("");
                        player.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        player.sendMessage("  §b§lInformações do VIP");
                        player.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                        if (sub.isPresent()) {
                            VipStorage.VipSubscription vip = sub.get();
                            player.sendMessage("  §7Tier: §f" + vip.tier());
                            player.sendMessage("  §7Expira em: §e" + DATE_FMT.format(Instant.ofEpochSecond(vip.expiresAt())));

                            long remaining = vip.expiresAt() - System.currentTimeMillis() / 1000L;
                            if (remaining > 0) {
                                long days = remaining / 86400;
                                long hours = (remaining % 86400) / 3600;
                                player.sendMessage("  §7Restante: §a" + days + "d " + hours + "h");
                            } else {
                                player.sendMessage("  §7Status: §cExpirado");
                            }
                        } else {
                            player.sendMessage("  §7Tier: §cNenhum");
                            player.sendMessage("  §7Compre um VIP no Discord!");
                        }

                        boolean autoRenew = storage.getAutoRenew(uuid);
                        player.sendMessage("");
                        player.sendMessage("  §7Renovação automática: " + (autoRenew ? "§aAtivada" : "§cDesativada"));
                        player.sendMessage("  §7Use §f/vipconfig autoresnovar §7para alterar.");

                        boolean autoRepair = storage.getAutoRepair(uuid);
                        player.sendMessage("");
                        player.sendMessage("  §7Auto-reparo: " + (autoRepair ? "§aAtivado" : "§cDesativado"));
                        player.sendMessage("  §7Use §f/vipconfig autoreparo §7para alterar.");

                        player.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        player.sendMessage("");
                    };

                    if (org.bukkit.Bukkit.isPrimaryThread()) {
                        send.run();
                    } else {
                        org.bukkit.Bukkit.getScheduler().runTask(module.plugin(), send);
                    }
                });
    }

    private void toggleAutoRenew(Player player) {
        UUID uuid = player.getUniqueId();
        VipStorage storage = module.vipStorage();

        CompletableFuture.supplyAsync(() -> {
            VipStorage.VipSubscription sub = storage.getActiveSubscription(uuid).orElse(null);
            if (sub == null) {
                return "NO_VIP";
            }
            boolean newValue = storage.toggleAutoRenew(uuid);
            return newValue ? "ON" : "OFF";
        }).thenAccept(result -> {
            Runnable send = () -> {
                switch (result) {
                    case "NO_VIP" -> player.sendMessage("§8[§bHaizCore§8] §cVocê não possui um VIP ativo.");
                    case "ON" -> {
                        player.sendMessage("§8[§bHaizCore§8] §aRenovação automática §lativada§a!");
                        player.sendMessage("§7Seu VIP será renovado automaticamente antes de expirar.");
                    }
                    case "OFF" -> {
                        player.sendMessage("§8[§bHaizCore§8] §eRenovação automática §ldesativada§e.");
                        player.sendMessage("§7Seu VIP não será renovado automaticamente.");
                    }
                }
            };
            if (org.bukkit.Bukkit.isPrimaryThread()) {
                send.run();
            } else {
                org.bukkit.Bukkit.getScheduler().runTask(module.plugin(), send);
            }
        });
    }

    private void toggleAutoRepair(Player player) {
        UUID uuid = player.getUniqueId();
        VipStorage storage = module.vipStorage();

        CompletableFuture.supplyAsync(() -> {
            VipStorage.VipSubscription sub = storage.getActiveSubscription(uuid).orElse(null);
            if (sub == null) {
                return "NO_VIP";
            }
            if (!module.autoRepairManager().isEnabled()) {
                return "DISABLED";
            }
            if (!module.autoRepairManager().isEligibleTier(sub.tier())) {
                return "NOT_ELIGIBLE";
            }
            boolean newValue = storage.toggleAutoRepair(uuid);
            return newValue ? "ON" : "OFF";
        }).thenAccept(result -> {
            Runnable send = () -> {
                switch (result) {
                    case "NO_VIP" -> player.sendMessage("§8[§bHaizCore§8] §cVocê não possui um VIP ativo.");
                    case "DISABLED" -> player.sendMessage("§8[§bHaizCore§8] §cAuto-reparo está desativado no servidor.");
                    case "NOT_ELIGIBLE" -> player.sendMessage("§8[§bHaizCore§8] §cSeu tier de VIP não possui auto-reparo.");
                    case "ON" -> {
                        player.sendMessage("§8[§bHaizCore§8] §aAuto-reparo §lativado§a!");
                        player.sendMessage("§7Suas ferramentas serão reparadas automaticamente.");
                    }
                    case "OFF" -> {
                        player.sendMessage("§8[§bHaizCore§8] §eAuto-reparo §ldesativado§e.");
                        player.sendMessage("§7Suas ferramentas não serão reparadas automaticamente.");
                    }
                }
            };
            if (org.bukkit.Bukkit.isPrimaryThread()) {
                send.run();
            } else {
                org.bukkit.Bukkit.getScheduler().runTask(module.plugin(), send);
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("status", "autoresnovar", "autoreparo").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return Collections.emptyList();
    }
}
