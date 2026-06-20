package com.haiz.servercore.vip;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class VipEmbedFactory {

    private static final Color GOLD   = new Color(0xFFAC30);
    private static final Color GREEN  = new Color(0x2ECC71);
    private static final Color RED    = new Color(0xE74C3C);
    private static final Color BLUE   = new Color(0x5865F2);
    private static final Color PURPLE = new Color(0x9B59B6);

    private VipEmbedFactory() {}

    // ── Mensagem principal da loja ────────────────────────────────────────────

    public static MessageCreateData shopMessage(VipConfig cfg) {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(GOLD)
                .setDescription("""
                        ## — ⭐ » VIPs do Servidor
                        -# **Apoie o servidor e desbloqueie vantagens exclusivas!**

                        Ao adquirir um VIP, você ajuda o servidor a continuar online, receber melhorias e ganhar novos sistemas para todos os jogadores.
                        ### 🛒 » Como comprar?
                        Selecione um VIP no menu abaixo para ver os detalhes e iniciar a compra.

                        -# ⚠️ Caso já possua um VIP, você também pode melhorar seu plano atual.""")
                .setImage(cfg.shopImageUrl())
                .setThumbnail(cfg.shopThumbnailUrl())
                .setFooter("Haiz Network • VIP Shop")
                .setTimestamp(Instant.now())
                .build();

        // Dropdown de VIPs
        List<SelectOption> options = new ArrayList<>();
        for (VipConfig.VipTier tier : cfg.tiers()) {
            SelectOption opt = SelectOption.of(tier.displayName(), tier.id())
                    .withDescription(tier.description());
            if (!tier.emojiId().isBlank()) {
                opt = opt.withEmoji(Emoji.fromCustom(tier.emojiName(), Long.parseLong(tier.emojiId()), false));
            }
            options.add(opt);
        }

        StringSelectMenu menu = StringSelectMenu.create("haiz:vip:select")
                .setPlaceholder("🌟 Escolha um VIP para ver detalhes")
                .setMinValues(1).setMaxValues(1)
                .addOptions(options)
                .build();

        return new MessageCreateBuilder()
                .addEmbeds(embed)
                .addComponents(ActionRow.of(menu))
                .build();
    }

    // ── Detalhes de um VIP específico ────────────────────────────────────────

    public static MessageCreateData tierDetail(VipConfig.VipTier tier) {
        StringBuilder perks = new StringBuilder();
        for (String p : tier.perks()) perks.append("• ").append(p).append('\n');

        MessageEmbed embed = new EmbedBuilder()
                .setColor(GOLD)
                .setTitle("⭐ VIP " + tier.displayName())
                .setDescription(tier.description())
                .addField("💰 Preço", tier.price() + " MobCoins", true)
                .addField("🎁 Benefícios", perks.isEmpty() ? "Sem descrição." : perks.toString(), false)
                .setFooter("Haiz Network • VIP Shop")
                .setTimestamp(Instant.now())
                .build();

        return new MessageCreateBuilder()
                .addEmbeds(embed)
                .addComponents(ActionRow.of(
                        Button.success("haiz:vip:buy:" + tier.id(), "✅ Comprar por " + tier.price() + " MC"),
                        Button.secondary("haiz:vip:back", "← Voltar")
                ))
                .build();
    }

    // ── Confirmação de compra ─────────────────────────────────────────────────

    public static MessageCreateData purchaseConfirm(VipConfig.VipTier tier, String mcName, double balance) {
        String balanceStr = balance < 0 ? "Desconhecido" : String.format("%.0f MC", balance);
        MessageEmbed embed = new EmbedBuilder()
                .setColor(GOLD)
                .setTitle("🛒 Confirmar Compra")
                .setDescription("Você está prestes a comprar o **VIP " + tier.displayName() + "**.")
                .addField("👤 Conta Minecraft", "`" + mcName + "`", true)
                .addField("💰 Preço", tier.price() + " MobCoins", true)
                .addField("💳 Seu saldo", balanceStr, true)
                .setFooter("Esta ação é irreversível após a confirmação.")
                .setTimestamp(Instant.now())
                .build();

        return new MessageCreateBuilder()
                .addEmbeds(embed)
                .addComponents(ActionRow.of(
                        Button.success("haiz:vip:confirm", "✅ Confirmar"),
                        Button.danger("haiz:vip:cancel", "❌ Cancelar")
                ))
                .build();
    }

    // ── Solicitar vinculação ──────────────────────────────────────────────────

    public static MessageCreateData linkRequired() {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(BLUE)
                .setTitle("🔗 Vincule sua conta Minecraft")
                .setDescription("""
                        Para comprar um VIP, você precisa vincular sua conta do Discord ao Minecraft.

                        **Como vincular:**
                        1️⃣ Esteja online no servidor Minecraft
                        2️⃣ Use o comando abaixo com seu nick exato:
                        ```
                        /verificar <seu nick>
                        ```
                        3️⃣ Você receberá um código no chat do Minecraft
                        4️⃣ Digite `/linkar <código>` no Minecraft para confirmar""")
                .setFooter("Haiz Network • Verificação")
                .setTimestamp(Instant.now())
                .build();

        return new MessageCreateBuilder().addEmbeds(embed).build();
    }

    // ── Resultados ────────────────────────────────────────────────────────────

    public static MessageCreateData playerOffline(String nick) {
        return ephemeral(RED, "❌ Jogador offline",
                "O jogador **" + nick + "** não está online no servidor no momento.\nEntre no servidor e tente novamente.");
    }

    public static MessageCreateData codeSent(String nick, int expirySeconds) {
        return ephemeral(GREEN, "✅ Código enviado!",
                "Um código de verificação foi enviado para **" + nick + "** no chat do Minecraft.\n"
                        + "Digite `/linkar <código>` no servidor.\nExpira em **" + expirySeconds + " segundos**.");
    }

    public static MessageCreateData linkSuccess(String nick) {
        return ephemeral(GREEN, "🎉 Conta vinculada!",
                "Sua conta Discord foi vinculada com sucesso a **" + nick + "**!\nAgora você pode comprar VIPs.");
    }

    public static MessageCreateData purchaseSuccess(VipConfig.VipTier tier) {
        return ephemeral(GREEN, "🎉 Compra realizada!",
                "Você adquiriu o **VIP " + tier.displayName() + "** com sucesso!\n"
                        + "Suas vantagens já estão ativas no servidor. Obrigado pelo apoio! ❤️");
    }

    public static MessageCreateData insufficientFunds(VipConfig.VipTier tier, double balance) {
        String balStr = balance < 0 ? "desconhecido" : String.format("%.0f", balance);
        return ephemeral(RED, "❌ Saldo insuficiente",
                "Você precisa de **" + tier.price() + " MobCoins** para este VIP.\n"
                        + "Seu saldo atual: **" + balStr + " MobCoins**.");
    }

    public static MessageCreateData purchaseCancelled() {
        return ephemeral(RED, "❌ Compra cancelada", "Você cancelou a compra. Nenhuma cobrança foi realizada.");
    }

    public static MessageCreateData purchaseExpired() {
        return ephemeral(RED, "⏱️ Tempo esgotado", "A sessão de compra expirou. Selecione o VIP novamente.");
    }

    public static MessageCreateData error(String msg) {
        return ephemeral(RED, "⚠️ Erro", msg);
    }

    // ── Log de compra (canal de logs) ─────────────────────────────────────────

    public static MessageEmbed purchaseLog(String discordTag, String mcName,
                                           VipConfig.VipTier tier, long price) {
        return new EmbedBuilder()
                .setColor(PURPLE)
                .setTitle("🛍️ Nova compra de VIP")
                .addField("Discord", discordTag, true)
                .addField("Minecraft", mcName, true)
                .addField("VIP", tier.displayName(), true)
                .addField("Preço", price + " MobCoins", true)
                .setTimestamp(Instant.now())
                .setFooter("Haiz Network • VIP Shop")
                .build();
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private static MessageCreateData ephemeral(Color color, String title, String desc) {
        return new MessageCreateBuilder()
                .addEmbeds(new EmbedBuilder()
                        .setColor(color).setTitle(title).setDescription(desc)
                        .setTimestamp(Instant.now()).build())
                .build();
    }
}