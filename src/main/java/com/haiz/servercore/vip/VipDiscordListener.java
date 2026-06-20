package com.haiz.servercore.vip;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Listener JDA para o sistema de loja de VIPs.
 *
 * Eventos tratados:
 *  - /verificar <nick>           → inicia vínculo
 *  - StringSelect haiz:vip:select → mostra detalhes do VIP
 *  - Button haiz:vip:buy:<id>   → inicia confirmação de compra
 *  - Button haiz:vip:confirm    → executa compra
 *  - Button haiz:vip:cancel     → cancela compra
 *  - Button haiz:vip:back       → volta para a loja
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
                ? event.getOption("nick").getAsString()
                : null;

        if (nick == null || nick.isBlank()) {
            event.reply("❌ Informe seu nick do Minecraft.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue(hook -> {
            String discordId = event.getUser().getId();

            // Já está vinculado?
            if (module.linkManager().isLinked(discordId)) {
                Optional<String> mcName = module.linkManager().mcNameByDiscordId(discordId);
                hook.sendMessage("✅ Sua conta já está vinculada a **"
                        + mcName.orElse("desconhecido") + "**.").queue();
                return;
            }

            Optional<String> code = module.linkManager().initiateLink(discordId, nick);
            if (code.isEmpty()) {
                hook.sendMessageEmbeds(
                        VipEmbedFactory.playerOffline(nick).getEmbeds().get(0)).queue();
                return;
            }

            hook.sendMessageEmbeds(
                    VipEmbedFactory.codeSent(nick, module.vipConfig().linkCodeExpirySeconds())
                            .getEmbeds().get(0)).queue();
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
                hook.sendMessage("❌ VIP não encontrado.").queue();
                return;
            }

            // Não está vinculado? Pede para vincular antes.
            if (!module.linkManager().isLinked(discordId)) {
                hook.sendMessageEmbeds(
                        VipEmbedFactory.linkRequired().getEmbeds().get(0)).queue();
                return;
            }

            hook.sendMessage(VipEmbedFactory.tierDetail(tier.get())).queue();
        });
    }

    // ── Botões ────────────────────────────────────────────────────────────────

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.startsWith("haiz:vip:buy:")) {
            handleBuy(event, id.substring("haiz:vip:buy:".length()));
        } else if (id.equals("haiz:vip:confirm")) {
            handleConfirm(event);
        } else if (id.equals("haiz:vip:cancel")) {
            handleCancel(event);
        } else if (id.equals("haiz:vip:back")) {
            handleBack(event);
        }
    }

    private void handleBuy(ButtonInteractionEvent event, String tierId) {
        String discordId = event.getUser().getId();
        event.deferReply(true).queue(hook -> {
            Optional<VipConfig.VipTier> opt = module.vipConfig().find(tierId);
            if (opt.isEmpty()) { hook.sendMessage("❌ VIP inválido.").queue(); return; }
            VipConfig.VipTier tier = opt.get();

            Optional<UUID> uuidOpt = module.linkManager().uuidByDiscordId(discordId);
            Optional<String> mcNameOpt = module.linkManager().mcNameByDiscordId(discordId);
            if (uuidOpt.isEmpty()) {
                hook.sendMessageEmbeds(VipEmbedFactory.linkRequired().getEmbeds().get(0)).queue();
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
                hook.sendMessageEmbeds(VipEmbedFactory.purchaseExpired().getEmbeds().get(0)).queue();
                return;
            }

            PurchaseManager.PurchaseResult result = module.purchaseManager().executePurchase(discordId);

            switch (result) {
                case SUCCESS -> {
                    hook.sendMessage(VipEmbedFactory.purchaseSuccess(p.tier())).queue();
                    // Enviar log no canal de logs
                    sendLog(event.getUser().getAsTag(), p.mcName(), p.tier(), p.tier().price());
                    // Avisar no Minecraft
                    Bukkit.getScheduler().runTask(module.plugin(), () -> {
                        var player = Bukkit.getPlayerExact(p.mcName());
                        if (player != null) {
                            player.sendMessage("§8[§bHaizCore§8] §a§lVIP " + p.tier().displayName()
                                    + " §aativado! Obrigado pelo apoio! ♥");
                        }
                    });
                }
                case INSUFFICIENT_FUNDS -> {
                    double bal = module.mobCoins().getBalance(p.uuid());
                    hook.sendMessage(VipEmbedFactory.insufficientFunds(p.tier(), bal)).queue();
                }
                case EXPIRED -> hook.sendMessage(VipEmbedFactory.purchaseExpired()).queue();
                default -> hook.sendMessage(VipEmbedFactory.error("Erro ao processar compra. Contate um administrador.")).queue();
            }
        });
    }

    private void handleCancel(ButtonInteractionEvent event) {
        module.purchaseManager().cancelPurchase(event.getUser().getId());
        event.reply(VipEmbedFactory.purchaseCancelled()).setEphemeral(true).queue();
    }

    private void handleBack(ButtonInteractionEvent event) {
        event.reply(VipEmbedFactory.shopMessage(module.vipConfig())).setEphemeral(true).queue();
    }

    private void sendLog(String discordTag, String mcName, VipConfig.VipTier tier, long price) {
        String logChannelId = module.vipConfig().vipLogsChannelId();
        if (logChannelId.isBlank() || module.plugin().discord().isOnline() == false) return;
        var jda = module.plugin().discord().jda();
        if (jda == null) return;
        TextChannel ch = jda.getTextChannelById(logChannelId);
        if (ch != null) ch.sendMessageEmbeds(VipEmbedFactory.purchaseLog(discordTag, mcName, tier, price)).queue();
    }
}