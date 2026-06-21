package com.haiz.servercore.vip;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Listener JDA para o sistema de loja de VIPs.
 *
 * Botões "Comprar" dos embeds originais (custom_id legados):
 *   comprar_vip | comprar_elite | comprar_ultra | comprar_famoso | comprar_midia
 *
 * Botões internos do fluxo de compra:
 *   haiz:vip:buy:<tierId>   → inicia confirmação
 *   haiz:vip:confirm        → executa compra
 *   haiz:vip:cancel         → cancela
 *   haiz:vip:back           → volta ao menu
 *
 * Dropdown:
 *   haiz:vip:select
 *
 * Slash command:
 *   /verificar <nick>
 */
public final class VipDiscordListener extends ListenerAdapter {

    private final VipModule module;

    public VipDiscordListener(VipModule module) {
        this.module = module;
    }

    // ── /verificar <nick> ─────────────────────────────────────────────────────

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("verificar")) return;

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
                hook.sendMessage("✅ Sua conta já está vinculada a **" + mc + "**.").queue();
                return;
            }

            Optional<String> code = module.linkManager().initiateLink(discordId, nick);
            if (code.isEmpty()) {
                hook.sendMessage(VipEmbedFactory.playerOffline(nick)).queue();
                return;
            }

            hook.sendMessage(VipEmbedFactory.codeSent(nick, module.vipConfig().linkCodeExpirySeconds())).queue();
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
            if (tier.isEmpty()) { hook.sendMessage("❌ VIP não encontrado.").queue(); return; }

            if (!module.linkManager().isLinked(discordId)) {
                hook.sendMessage(VipEmbedFactory.linkRequired()).queue();
                return;
            }

            // Mostra embed fiel ao JSON original + botão Comprar
            hook.sendMessage(VipEmbedFactory.tierDetail(tier.get())).queue();
        });
    }

    // ── Botões ────────────────────────────────────────────────────────────────

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();

        // Botões legados dos embeds originais: comprar_vip, comprar_elite, etc.
        if (id.startsWith("comprar_")) {
            String rawTier = id.substring("comprar_".length()); // "vip", "elite", ...
            String tierId  = normalizeTierId(rawTier);
            handleBuyRequest(event, tierId);
            return;
        }

        // Botões internos do fluxo
        if (id.startsWith("haiz:vip:buy:")) {
            handleBuyRequest(event, id.substring("haiz:vip:buy:".length()));
        } else if (id.equals("haiz:vip:confirm")) {
            handleConfirm(event);
        } else if (id.equals("haiz:vip:cancel")) {
            handleCancel(event);
        } else if (id.equals("haiz:vip:back")) {
            handleBack(event);
        }
    }

    // ── Handlers internos ─────────────────────────────────────────────────────

    private void handleBuyRequest(ButtonInteractionEvent event, String tierId) {
        String discordId = event.getUser().getId();
        event.deferReply(true).queue(hook -> {
            // Se não está vinculado, pede verificação primeiro
            if (!module.linkManager().isLinked(discordId)) {
                hook.sendMessage(VipEmbedFactory.linkRequired()).queue();
                return;
            }

            Optional<VipConfig.VipTier> opt = module.vipConfig().find(tierId);
            if (opt.isEmpty()) { hook.sendMessage("❌ VIP inválido.").queue(); return; }
            VipConfig.VipTier tier = opt.get();

            Optional<UUID>   uuidOpt   = module.linkManager().uuidByDiscordId(discordId);
            Optional<String> mcNameOpt = module.linkManager().mcNameByDiscordId(discordId);
            if (uuidOpt.isEmpty()) {
                hook.sendMessage(VipEmbedFactory.linkRequired()).queue();
                return;
            }

            double balance = module.mobCoins().getBalance(uuidOpt.get());
            module.purchaseManager().startPurchase(discordId, uuidOpt.get(), mcNameOpt.orElse("?"), tier);
            hook.sendMessage(VipEmbedFactory.purchaseConfirm(tier, mcNameOpt.orElse("?"), balance)).queue();
        });
    }

    private void handleConfirm(ButtonInteractionEvent event) {
        String discordId = event.getUser().getId();
        event.deferReply(true).queue(hook -> {
            PurchaseManager.PendingPurchase p = module.purchaseManager().getPending(discordId);
            if (p == null) {
                hook.sendMessage(VipEmbedFactory.purchaseExpired()).queue();
                return;
            }

            PurchaseManager.PurchaseResult result = module.purchaseManager().executePurchase(discordId);

            switch (result) {
                case SUCCESS -> {
                    hook.sendMessage(VipEmbedFactory.purchaseSuccess(p.tier())).queue();
                    sendPurchaseLog(event.getUser().getAsTag(), p);
                    notifyMinecraft(p);
                }
                case INSUFFICIENT_FUNDS -> {
                    double bal = module.mobCoins().getBalance(p.uuid());
                    hook.sendMessage(VipEmbedFactory.insufficientFunds(p.tier(), bal)).queue();
                }
                case EXPIRED -> hook.sendMessage(VipEmbedFactory.purchaseExpired()).queue();
                default      -> hook.sendMessage(VipEmbedFactory.error(
                        "Erro ao processar a compra. Contate um administrador.")).queue();
            }
        });
    }

    private void handleCancel(ButtonInteractionEvent event) {
        module.purchaseManager().cancelPurchase(event.getUser().getId());
        event.deferReply(true).queue(hook -> {
            hook.sendMessage(VipEmbedFactory.purchaseCancelled()).queue();
        });
    }

    private void handleBack(ButtonInteractionEvent event) {
        event.deferReply(true).queue(hook -> {
            hook.sendMessage(VipEmbedFactory.shopMessage(module.vipConfig())).queue();
        });
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    /**
     * Normaliza o sufixo do custom_id legado para o ID usado no VipConfig.
     * comprar_vip → Vip | comprar_midia → Midia | comprar_famoso → Famoso
     */
    private String normalizeTierId(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        // Capitaliza primeira letra, mantém o resto em minúsculo
        // "vip" → "Vip", "midia" → "Midia", "famoso" → "Famoso"
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
            ch.sendMessageEmbeds(
                    VipEmbedFactory.purchaseLog(discordTag, p.mcName(), p.tier(), p.tier().price())
            ).queue();
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
