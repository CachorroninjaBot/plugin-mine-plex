package com.haiz.servercore.vip;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Listener JDA para o sistema de loja de VIPs.
 * Usa Components v2 para todas as mensagens da loja.
 */
public final class VipDiscordListener extends ListenerAdapter {

    private final VipModule module;

    public VipDiscordListener(VipModule module) {
        this.module = module;
    }

    // ── /verificar <nick> ─────────────────────────────────────────────────────

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "verificar" -> handleVerificar(event);
            case "vipconfig" -> handleVipConfig(event);
        }
    }

    private void handleVerificar(SlashCommandInteractionEvent event) {
        String nick = event.getOption("nick") != null
                ? event.getOption("nick").getAsString().trim()
                : null;

        if (nick == null || nick.isBlank()) {
            event.reply("❌ Informe seu nick do Minecraft.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue(hook -> {
            String discordId = event.getUser().getId();

            if (module.linkManager().isLinked(discordId)) {
                String mc = module.linkManager().mcNameByDiscordId(discordId).orElse("desconhecido");
                sendV2Reply(hook,
                        simpleV2(0x2ECC71, "✅ Já vinculado",
                                "Sua conta já está vinculada a **" + mc + "**."));
                return;
            }

            Optional<String> code = module.linkManager().initiateLink(discordId, nick);
            if (code.isEmpty()) {
                sendV2Reply(hook, VipEmbedFactory.playerOfflineV2(nick));
                return;
            }

            sendV2Reply(hook,
                    VipEmbedFactory.codeSentV2(nick, module.vipConfig().linkCodeExpirySeconds()));
        });
    }

    private void handleVipConfig(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(hook -> {
            String discordId = event.getUser().getId();
            Optional<UUID> uuidOpt = module.linkManager().uuidByDiscordId(discordId);

            if (uuidOpt.isEmpty()) {
                sendV2Reply(hook, VipEmbedFactory.linkRequiredV2());
                return;
            }

            UUID uuid = uuidOpt.get();
            Optional<String> mcNameOpt = module.linkManager().mcNameByDiscordId(discordId);
            String mcName = mcNameOpt.orElse("desconhecido");

            VipStorage.VipSubscription sub = module.vipStorage().getActiveSubscription(uuid).orElse(null);
            boolean autoRenew = module.vipStorage().getAutoRenew(uuid);
            double balance = module.mobCoins().getBalance(uuid);

            sendV2Reply(hook, VipEmbedFactory.vipConfigV2(mcName, sub, autoRenew, balance));
        });
    }

    // ── Dropdown: seleção de VIP ──────────────────────────────────────────────

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("haiz:vip:select")) return;

        String tierId = event.getValues().get(0);
        String discordId = event.getUser().getId();

        event.deferReply(true).queue(hook -> {
            Optional<VipConfig.VipTier> tier = module.vipConfig().find(tierId);
            if (tier.isEmpty()) {
                sendV2Reply(hook, simpleV2(0xE74C3C, "❌ VIP não encontrado", "Selecione um VIP válido no menu."));
                return;
            }

            if (!module.linkManager().isLinked(discordId)) {
                sendV2Reply(hook, VipEmbedFactory.linkRequiredV2());
                return;
            }

            // Envia detalhe do tier via Components v2
            DataObject v2msg = VipEmbedFactory.tierDetailV2(tier.get());
            sendV2Reply(hook, v2msg);
        });
    }

    // ── Botões ────────────────────────────────────────────────────────────────

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.startsWith("comprar_")) {
            String rawTier = id.substring("comprar_".length());
            String tierId = normalizeTierId(rawTier);
            handleBuyRequest(event, tierId);
            return;
        }

        if (id.startsWith("haiz:vip:buy:")) {
            handleBuyRequest(event, id.substring("haiz:vip:buy:".length()));
        } else if (id.equals("haiz:vip:confirm")) {
            handleConfirm(event);
        } else if (id.equals("haiz:vip:cancel")) {
            handleCancel(event);
        } else if (id.equals("haiz:vip:back")) {
            handleBack(event);
        } else if (id.equals("haiz:vip:toggle-renew")) {
            handleToggleRenew(event);
        } else if (id.equals("haiz:vip:toggle-repair")) {
            handleToggleRepair(event);
        }
    }

    // ── Handlers internos ─────────────────────────────────────────────────────

    private void handleBuyRequest(ButtonInteractionEvent event, String tierId) {
        String discordId = event.getUser().getId();
        event.deferReply(true).queue(hook -> {
            if (!module.linkManager().isLinked(discordId)) {
                sendV2Reply(hook, VipEmbedFactory.linkRequiredV2());
                return;
            }

            Optional<VipConfig.VipTier> opt = module.vipConfig().find(tierId);
            if (opt.isEmpty()) { hook.sendMessage("❌ VIP inválido.").queue(); return; }
            VipConfig.VipTier tier = opt.get();

            Optional<UUID> uuidOpt = module.linkManager().uuidByDiscordId(discordId);
            Optional<String> mcNameOpt = module.linkManager().mcNameByDiscordId(discordId);
            if (uuidOpt.isEmpty()) {
                sendV2Reply(hook, VipEmbedFactory.linkRequiredV2());
                return;
            }

            double balance = module.mobCoins().getBalance(uuidOpt.get());
            module.purchaseManager().startPurchase(discordId, uuidOpt.get(), mcNameOpt.orElse("?"), tier);
            sendV2Reply(hook, VipEmbedFactory.purchaseConfirmV2(tier, mcNameOpt.orElse("?"), balance));
        });
    }

    private void handleConfirm(ButtonInteractionEvent event) {
        String discordId = event.getUser().getId();
        event.deferReply(true).queue(hook -> {
            PurchaseManager.PendingPurchase p = module.purchaseManager().getPending(discordId);
            if (p == null) {
                sendV2Reply(hook, VipEmbedFactory.purchaseExpiredV2());
                return;
            }

            PurchaseManager.PurchaseResult result = module.purchaseManager().executePurchase(discordId);

            switch (result) {
                case SUCCESS, SUCCESS_UPGRADE -> {
                    sendV2Reply(hook, VipEmbedFactory.purchaseSuccessV2(p.tier()));
                    sendPurchaseLog(event.getUser().getAsTag(), p);
                    notifyMinecraft(p);
                }
                case SAME_TIER -> sendV2Reply(hook, VipEmbedFactory.errorV2(
                        "Você já possui este plano VIP ativo."));
                case INSUFFICIENT_FUNDS -> {
                    double bal = module.mobCoins().getBalance(p.uuid());
                    sendV2Reply(hook, VipEmbedFactory.insufficientFundsV2(p.tier(), bal));
                }
                case EXPIRED -> sendV2Reply(hook, VipEmbedFactory.purchaseExpiredV2());
                default -> sendV2Reply(hook, VipEmbedFactory.errorV2(
                        "Erro ao processar a compra. Contate um administrador."));
            }
        });
    }

    private void handleCancel(ButtonInteractionEvent event) {
        module.purchaseManager().cancelPurchase(event.getUser().getId());
        event.deferReply(true).queue(hook -> {
            sendV2Reply(hook, VipEmbedFactory.purchaseCancelledV2());
        });
    }

    private void handleBack(ButtonInteractionEvent event) {
        event.deferReply(true).queue(hook -> {
            DataObject v2msg = VipEmbedFactory.shopMessageV2(module.vipConfig());
            sendV2Reply(hook, v2msg);
        });
    }

    // ── Envio V2 ─────────────────────────────────────────────────────────────

    private void handleToggleRenew(ButtonInteractionEvent event) {
        event.deferReply(true).queue(hook -> {
            String discordId = event.getUser().getId();
            Optional<UUID> uuidOpt = module.linkManager().uuidByDiscordId(discordId);

            if (uuidOpt.isEmpty()) {
                sendV2Reply(hook, VipEmbedFactory.linkRequiredV2());
                return;
            }

            UUID uuid = uuidOpt.get();
            VipStorage.VipSubscription sub = module.vipStorage().getActiveSubscription(uuid).orElse(null);
            if (sub == null) {
                sendV2Reply(hook, simpleV2(0xE74C3C, "❌ Sem VIP ativo",
                        "Você não possui um VIP ativo para configurar renovação."));
                return;
            }

            boolean newAutoRenew = module.vipStorage().toggleAutoRenew(uuid);
            boolean autoRenew = module.vipStorage().getAutoRenew(uuid);
            double balance = module.mobCoins().getBalance(uuid);
            String mcName = module.linkManager().mcNameByDiscordId(discordId).orElse("?");

            sendV2Reply(hook, VipEmbedFactory.vipConfigV2(mcName, sub, autoRenew, balance));
        });
    }

    private void handleToggleRepair(ButtonInteractionEvent event) {
        event.deferReply(true).queue(hook -> {
            String discordId = event.getUser().getId();
            Optional<UUID> uuidOpt = module.linkManager().uuidByDiscordId(discordId);

            if (uuidOpt.isEmpty()) {
                sendV2Reply(hook, VipEmbedFactory.linkRequiredV2());
                return;
            }

            UUID uuid = uuidOpt.get();

            if (!module.autoRepairManager().isEnabled()) {
                sendV2Reply(hook, simpleV2(0xE74C3C, "❌ Auto-repair desativado",
                        "Auto-repair está desativado no servidor."));
                return;
            }

            if (module.autoRepairManager().isApplyToAll()) {
                sendV2Reply(hook, simpleV2(0x2ECC71, "🔧 Auto-repair ativo",
                        "Auto-repair está ativo para todos os jogadores."));
                return;
            }

            VipStorage.VipSubscription sub = module.vipStorage().getActiveSubscription(uuid).orElse(null);
            if (sub == null) {
                sendV2Reply(hook, simpleV2(0xE74C3C, "❌ Sem VIP ativo",
                        "Você não possui um VIP ativo para configurar auto-repair."));
                return;
            }

            boolean newValue = module.vipStorage().toggleAutoRepair(uuid);
            boolean autoRepair = module.vipStorage().getAutoRepair(uuid);

            String status = autoRepair ? "🟢 **Ativado**" : "🔴 **Desativado**";
            sendV2Reply(hook, simpleV2(autoRepair ? 0x2ECC71 : 0xE74C3C,
                    "🔧 Auto-repair " + (autoRepair ? "ativado" : "desativado"),
                    "Auto-repair: " + status + "\n"
                    + "Quando seu item estiver quase quebrando, ele será reparado automaticamente."));
        });
    }

    /**
     * Envia uma mensagem Components v2 como resposta a uma interação via REST API raw.
     */
    private void sendV2Reply(net.dv8tion.jda.api.interactions.InteractionHook hook, DataObject v2msg) {
        try {
            V2Messenger.replyInteraction(hook.getJDA(), hook.getInteraction().getToken(), v2msg)
                    .thenAccept(status -> {
                        if (status < 200 || status >= 300) {
                            module.plugin().getLogger().warning("[VipShop] V2 reply retornou HTTP " + status);
                            hook.sendMessage("⚠️ Erro ao carregar. Tente novamente.").queue();
                        }
                    })
                    .exceptionally(e -> {
                        module.plugin().getLogger().warning("[VipShop] Falha ao enviar V2: " + e.getMessage());
                        hook.sendMessage("⚠️ Erro ao carregar. Tente novamente.").queue();
                        return null;
                    });
        } catch (Exception e) {
            module.plugin().getLogger().warning("[VipShop] Falha ao enviar V2: " + e.getMessage());
            hook.sendMessage("⚠️ Erro ao carregar. Tente novamente.").queue();
        }
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    private DataObject simpleV2(int color, String title, String description) {
        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(color, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "## " + title + "\n" + description));
        });
        return builder.build();
    }

    private String normalizeTierId(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        String lower = raw.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private void sendPurchaseLog(String discordTag, PurchaseManager.PendingPurchase p) {
        String logChannelId = module.vipConfig().vipLogsChannelId();
        if (logChannelId.isBlank()) return;
        var jda = module.plugin().discord().jda();
        if (jda == null) return;
        TextChannel ch = jda.getTextChannelById(logChannelId);
        if (ch != null) {
            V2Messenger.send(ch, VipEmbedFactory.purchaseLogV2(discordTag, p.mcName(), p.tier(), p.tier().price()));
        }
    }

    private void notifyMinecraft(PurchaseManager.PendingPurchase p) {
        Bukkit.getScheduler().runTask(module.plugin(), () -> {
            var player = Bukkit.getPlayerExact(p.mcName());
            if (player != null) {
                player.sendMessage("");
                player.sendMessage("§8[§bHaizCore§8] §a§lVIP " + p.tier().displayName() + " ativado!");
                player.sendMessage("§7Obrigado por apoiar o servidor. Suas vantagens já estão ativas. §c♥");
                player.sendMessage("");
            }
        });
    }
}
