package com.haiz.servercore.vip;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.haiz.servercore.vip.V2MessageBuilder.*;

public final class VipEmbedFactory {

    // Cores
    private static final int C_VIP    = 0xF8A016;
    private static final int C_ELITE  = 0xED595A;
    private static final int C_ULTRA  = 0x803EE2;
    private static final int C_FAMOSO = 0x0E43ED;
    private static final int C_MIDIA  = 0x0EB66A;
    private static final int C_SHOP   = 0xFFAC30;
    private static final int C_GREEN  = 0x2ECC71;
    private static final int C_RED    = 0xE74C3C;
    private static final int C_BLUE   = 0x5865F2;
    private static final int C_PURPLE = 0x9B59B6;

    private static final Emoji EMOJI_CART = Emoji.fromCustom("Carrinho", 1517986569380691993L, false);

    private VipEmbedFactory() {}

    // ══════════════════════════════════════════════════════════════════════
    //  COMPONENTS V2 — Loja principal
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Mensagem da loja usando Components v2.
     * Retorna o DataObject raw para envio via V2Messenger.
     */
    public static DataObject shopMessageV2(VipConfig cfg) {
        V2MessageBuilder builder = V2MessageBuilder.create();

        // Header com thumbnail do servidor
        builder.addContainer(C_SHOP, children -> {
            children.add(DataObject.empty()
                    .put("type", 9) // Section
                    .put("components", net.dv8tion.jda.api.utils.data.DataArray.fromCollection(List.of(
                            DataObject.empty().put("type", 10).put("content",
                                    "## — <:Star3:1498537712225878178> » VIPs do Servidor\n"
                                    + "-# **Apoie o servidor e desbloqueie vantagens exclusivas!**\n\n"
                                    + "Ao adquirir um VIP, você ajuda o servidor a continuar online, "
                                    + "receber melhorias e ganhar novos sistemas para todos os jogadores.")
                    )))
                    .put("accessory", thumbnail(cfg.shopThumbnailUrl(), "Haiz Network")));

            children.add(separator(false));
            children.add(separator(false));

            children.add(DataObject.empty()
                    .put("type", 10)
                    .put("content", "### 🛒 » Como comprar?\n"
                            + "Selecione um VIP no menu abaixo para ver os detalhes e iniciar a compra.\n\n"
                            + "-# <:iconswarning:1516640554576904202> Caso já possua um VIP, "
                            + "você também pode melhorar seu plano atual."));

            // Imagem da loja
            if (cfg.shopImageUrl() != null && !cfg.shopImageUrl().isBlank()) {
                children.add(DataObject.empty()
                        .put("type", 12) // Media Gallery
                        .put("items", net.dv8tion.jda.api.utils.data.DataArray.fromCollection(List.of(
                                DataObject.empty().put("url", cfg.shopImageUrl())
                        ))));
            }

            // Select menu
            List<DataObject> options = new ArrayList<>();
            for (VipConfig.VipTier tier : cfg.tiers()) {
                DataObject opt = selectOption(tier.displayName(), tier.id(), tier.description(),
                        tier.emojiName(), tier.emojiId().isBlank() ? null : Long.parseLong(tier.emojiId()));
                options.add(opt);
            }
            children.add(actionRow(stringSelect("haiz:vip:select",
                    "🌟 Escolha um VIP para ver detalhes", options)));
        });

        return builder.build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  COMPONENTS V2 — Detalhes por tier
    // ══════════════════════════════════════════════════════════════════════

    public static DataObject tierDetailV2(VipConfig.VipTier tier) {
        return switch (tier.id()) {
            case "Vip"    -> vipDetailV2();
            case "Elite"  -> eliteDetailV2();
            case "Ultra"  -> ultraDetailV2();
            case "Famoso" -> famosoDetailV2();
            case "Midia"  -> midiaDetailV2();
            default       -> genericDetailV2(tier);
        };
    }

    private static DataObject vipDetailV2() {
        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(C_VIP, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "## — <:vip:1516290472979595436> » VIP\n"
                    + "-# Plano inicial para quem quer mais conforto, limites maiores e comandos exclusivos.\n\n"
                    + "### ✨ » Destaques\n"
                    + "• Teleporte sem tempo de espera\n"
                    + "• Reparo sem tempo de espera\n"
                    + "• Recompensa diária exclusiva\n"
                    + "• Acesso a comandos especiais"));
            children.add(separator());
            children.add(DataObject.empty().put("type", 10).put("content",
                    "### <a:XP:1490100752167862324> » Limites\n"
                    + "• **1** Mascote • **5** Profissões • **50** Minions\n"
                    + "• **20** Blocos de claim • **60** Lojas de baú\n"
                    + "• **40** `/pv` • **30** `/pw` • **20** Missões ativas\n"
                    + "• **500** Leilões • **70** Casas"));
            children.add(separator());
            children.add(DataObject.empty().put("type", 10).put("content",
                    "### <:Console:1406969852765405266> » Comandos\n"
                    + "• `/ps flag` • `/anvil` `/nick` `/glow`\n"
                    + "• `/feed` `/heal` `/craft` • `/enderchest` `/kit`\n"
                    + "• `/ignore` `/itemname` `/hat`"));
            children.add(separator());
            children.add(actionRow(buyButton("Vip")));
        });
        return builder.build();
    }

    private static DataObject eliteDetailV2() {
        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(C_ELITE, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "## — <:elite:1516290514847269014> » Elite\n"
                    + "-# Plano avançado para jogadores que querem mais limites, recursos especiais e mais conforto.\n\n"
                    + "### ✨ » Destaques\n"
                    + "• Teleporte sem tempo de espera\n"
                    + "• Reparo sem tempo de espera\n"
                    + "• Sem desconexão por AFK\n"
                    + "• Mantém XP ao morrer\n"
                    + "• Recompensa diária exclusiva"));
            children.add(separator());
            children.add(DataObject.empty().put("type", 10).put("content",
                    "### <a:XP:1490100752167862324> » Limites\n"
                    + "• **2** Mascotes • **8** Profissões • **100** Minions\n"
                    + "• **30** Blocos de claim • **70** Lojas de baú\n"
                    + "• **50** `/pv` • **40** `/pw` • **25** Missões ativas\n"
                    + "• **700** Leilões • **100** Casas"));
            children.add(separator());
            children.add(DataObject.empty().put("type", 10).put("content",
                    "### <:Console:1406969852765405266> » Comandos\n"
                    + "• `/ps flag` • `/anvil` `/nick` `/glow`\n"
                    + "• `/feed` `/heal` `/craft` • `/enderchest` `/kit`\n"
                    + "• `/ignore` `/itemname` `/hat`"));
            children.add(separator());
            children.add(actionRow(buyButton("Elite")));
        });
        return builder.build();
    }

    private static DataObject ultraDetailV2() {
        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(C_ULTRA, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "## — <:ultra:1516290555653390448> » Ultra\n"
                    + "-# Plano superior com limites altos, recursos avançados e comandos especiais.\n\n"
                    + "### ✨ » Destaques\n"
                    + "• Sem limite de minions e profissões\n"
                    + "• Teleporte sem tempo de espera\n"
                    + "• Sem desconexão por AFK\n"
                    + "• Mantém XP ao morrer\n"
                    + "• Acesso ao comando `/fly`"));
            children.add(separator());
            children.add(DataObject.empty().put("type", 10).put("content",
                    "### <a:XP:1490100752167862324> » Limites\n"
                    + "• **4** Mascotes • **50** Blocos de claim • **100** Lojas de baú\n"
                    + "• **60** `/pv` • **50** `/pw` • **30** Missões ativas\n"
                    + "• **1000** Leilões • **1000** Casas"));
            children.add(separator());
            children.add(DataObject.empty().put("type", 10).put("content",
                    "### <:Console:1406969852765405266> » Comandos\n"
                    + "• `/ps flag` `/ptime` • `/anvil` `/nick` `/glow`\n"
                    + "• `/feed` `/heal` `/craft` • `/enderchest` `/kit`\n"
                    + "• `/ignore` `/itemname` `/fly` `/hat`"));
            children.add(separator());
            children.add(actionRow(buyButton("Ultra")));
        });
        return builder.build();
    }

    private static DataObject famosoDetailV2() {
        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(C_FAMOSO, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "## — <:famoso:1516639945358577756> » Famoso\n"
                    + "-# Cargo especial para jogadores reconhecidos dentro do servidor.\n\n"
                    + "✨ **Inclui todos os benefícios do Ultra**\n"
                    + "🌟 **Destaque especial como Famoso**\n"
                    + "💎 **Ideal para quem quer mais reconhecimento no servidor**"));
            children.add(separator());
            children.add(actionRow(buyButton("Famoso")));
        });
        return builder.build();
    }

    private static DataObject midiaDetailV2() {
        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(C_MIDIA, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "## — <:midia:1516639500317626469> » Mídia\n"
                    + "-# Cargo especial para criadores de conteúdo e divulgadores do servidor.\n\n"
                    + "### ✨ » Benefícios\n"
                    + "• Todos os benefícios do **Elite**\n"
                    + "• Cargo exclusivo de **Mídia**\n"
                    + "• Reconhecimento como criador de conteúdo\n"
                    + "• Ideal para quem grava, posta ou divulga o servidor"));
            children.add(separator());
            children.add(actionRow(buyButton("Midia")));
        });
        return builder.build();
    }

    private static DataObject genericDetailV2(VipConfig.VipTier tier) {
        StringBuilder perks = new StringBuilder();
        for (String p : tier.perks()) perks.append("• ").append(p).append('\n');

        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(C_SHOP, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "## ⭐ VIP " + tier.displayName() + "\n"
                    + "-# " + tier.description() + "\n\n"
                    + "### 💰 Preço\n" + tier.price() + " MobCoins\n\n"
                    + "### 🎁 Benefícios\n" + (perks.isEmpty() ? "Sem descrição." : perks)));
            children.add(separator());
            children.add(actionRow(buyButton(tier.id())));
        });
        return builder.build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  COMPONENTS V2 — Confirmação de compra
    // ══════════════════════════════════════════════════════════════════════

    public static DataObject purchaseConfirmV2(VipConfig.VipTier tier, String mcName, double balance) {
        String balanceStr = balance < 0 ? "Desconhecido" : String.format("%.0f MC", balance);
        int color = tierColor(tier.id());

        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(color, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "## 🛒 Confirmar Compra — " + tier.displayName() + "\n"
                    + "Você está prestes a comprar o **VIP " + tier.displayName() + "**.\n"
                    + "Esta ação **não pode ser desfeita**."));
            children.add(separator());
            children.add(DataObject.empty().put("type", 10).put("content",
                    "**👤 Conta Minecraft:** `" + mcName + "`\n"
                    + "**💰 Preço:** " + tier.price() + " MobCoins\n"
                    + "**💳 Seu saldo:** " + balanceStr));
            children.add(separator());
            children.add(actionRow(
                    button(3, "haiz:vip:confirm", "✅ Confirmar"),
                    button(4, "haiz:vip:cancel", "❌ Cancelar")
            ));
        });
        return builder.build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  COMPONENTS V2 — Vinculação
    // ══════════════════════════════════════════════════════════════════════

    public static DataObject linkRequiredV2() {
        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(C_BLUE, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "## 🔗 Vincule sua conta Minecraft\n"
                    + "Para comprar um VIP, você precisa vincular sua conta do Discord ao Minecraft.\n\n"
                    + "**Como vincular:**\n"
                    + "1️⃣ Esteja **online** no servidor Minecraft\n"
                    + "2️⃣ Use `/verificar <seu nick>` aqui no Discord\n"
                    + "3️⃣ Você receberá um **código** no chat do Minecraft\n"
                    + "4️⃣ Digite `/linkar <código>` no Minecraft para confirmar"));
        });
        return builder.build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  COMPONENTS V2 — Mensagens de resultado
    // ══════════════════════════════════════════════════════════════════════

    public static DataObject playerOfflineV2(String nick) {
        return simpleV2(C_RED, "❌ Jogador offline",
                "**" + nick + "** não está online no servidor no momento.\n"
                + "Entre no servidor e tente novamente.");
    }

    public static DataObject codeSentV2(String nick, int expirySeconds) {
        return simpleV2(C_GREEN, "✅ Código enviado!",
                "Um código de verificação foi enviado para **" + nick + "** no chat do Minecraft.\n"
                + "Digite `/linkar <código>` no servidor.\nExpira em **" + expirySeconds + " segundos**.");
    }

    public static DataObject linkSuccessV2(String nick) {
        return simpleV2(C_GREEN, "🎉 Conta vinculada!",
                "Sua conta Discord foi vinculada com sucesso a **" + nick + "**!\n"
                + "Agora você pode comprar VIPs.");
    }

    public static DataObject purchaseSuccessV2(VipConfig.VipTier tier) {
        return simpleV2(C_GREEN, "🎉 Compra realizada!",
                "Você adquiriu o **VIP " + tier.displayName() + "** com sucesso!\n"
                + "Suas vantagens já estão ativas no servidor. Obrigado pelo apoio! ❤️");
    }

    public static DataObject insufficientFundsV2(VipConfig.VipTier tier, double balance) {
        String balStr = balance < 0 ? "desconhecido" : String.format("%.0f", balance);
        return simpleV2(C_RED, "❌ Saldo insuficiente",
                "Você precisa de **" + tier.price() + " MobCoins** para este VIP.\n"
                + "Seu saldo atual: **" + balStr + " MobCoins**.");
    }

    public static DataObject purchaseCancelledV2() {
        return simpleV2(C_RED, "❌ Compra cancelada",
                "Você cancelou a compra. Nenhuma cobrança foi realizada.");
    }

    public static DataObject purchaseExpiredV2() {
        return simpleV2(C_RED, "⏱️ Tempo esgotado",
                "A sessão de compra expirou. Selecione o VIP novamente para reiniciar.");
    }

    public static DataObject errorV2(String msg) {
        return simpleV2(C_RED, "⚠️ Erro", msg);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LOG DE COMPRA (Components v2)
    // ══════════════════════════════════════════════════════════════════════

    public static DataObject purchaseLogV2(String discordTag, String mcName,
                                            VipConfig.VipTier tier, long price) {
        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(C_PURPLE, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "### 🛍️ Nova compra de VIP\n"
                    + "**Discord:** " + discordTag + "\n"
                    + "**Minecraft:** `" + mcName + "`\n"
                    + "**VIP:** " + tier.displayName() + "\n"
                    + "**Preço:** " + price + " MobCoins"));
            children.add(DataObject.empty().put("type", 10).put("content",
                    "-# Haiz Network • VIP Shop • " + Instant.now()));
        });
        return builder.build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LOG DE COMPRA (mantém embed legacy para compatibilidade)
    // ══════════════════════════════════════════════════════════════════════

    public static MessageEmbed purchaseLog(String discordTag, String mcName,
                                           VipConfig.VipTier tier, long price) {
        return new EmbedBuilder()
                .setColor(C_PURPLE)
                .setTitle("🛍️ Nova compra de VIP")
                .addField("Discord", discordTag, true)
                .addField("Minecraft", "`" + mcName + "`", true)
                .addField("VIP", tier.displayName(), true)
                .addField("Preço", price + " MobCoins", true)
                .setTimestamp(Instant.now())
                .setFooter("Haiz Network • VIP Shop")
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  VIP CONFIG (Components v2)
    // ══════════════════════════════════════════════════════════════════════

    public static DataObject vipConfigV2(String mcName, VipStorage.VipSubscription sub,
                                          boolean autoRenew, double balance) {
        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(C_SHOP, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "## ⚙️ Configurações do VIP"));

            children.add(DataObject.empty().put("type", 14).put("spacing", 1).put("divider", true));

            String statusText;
            if (sub != null) {
                long remaining = sub.expiresAt() - System.currentTimeMillis() / 1000L;
                long days = remaining / 86400;
                long hours = (remaining % 86400) / 3600;
                String timeStr = remaining > 0 ? days + "d " + hours + "h" : "Expirado";

                statusText = "**Tier:** " + sub.tier() + "\n"
                        + "**Expira em:** <t:" + sub.expiresAt() + ":R>\n"
                        + "**Tempo restante:** " + timeStr;
            } else {
                statusText = "**Tier:** Nenhum VIP ativo\n"
                        + "Compre um VIP na loja acima!";
            }

            children.add(DataObject.empty().put("type", 10).put("content",
                    "👤 **Conta:** " + mcName + "\n"
                    + "💰 **Saldo:** " + (balance >= 0 ? String.format("%.0f", balance) + " MobCoins" : "Indisponível") + "\n\n"
                    + statusText));

            children.add(DataObject.empty().put("type", 14).put("spacing", 1).put("divider", true));

            String renewStatus = autoRenew ? "🟢 **Ativada**" : "🔴 **Desativada**";
            children.add(DataObject.empty().put("type", 10).put("content",
                    "🔄 **Renovação Automática:** " + renewStatus + "\n"
                    + "-# Quando ativada, seu VIP será renovado automaticamente antes de expirar."));

            DataObject renewButton = V2MessageBuilder.button(
                    autoRenew ? 4 : 3,
                    "haiz:vip:toggle-renew",
                    autoRenew ? "Desativar Renovação" : "Ativar Renovação");

            children.add(V2MessageBuilder.actionRow(renewButton));
        });
        return builder.build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FALLBACK LEGACY (para compatibilidade com JDA 5.0.0)
    // ══════════════════════════════════════════════════════════════════════

    public static MessageCreateData shopMessage(VipConfig cfg) {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(C_SHOP)
                .setDescription("""
                        ## — <:Star3:1498537712225878178> » VIPs do Servidor
                        -# **Apoie o servidor e desbloqueie vantagens exclusivas!**

                        Ao adquirir um VIP, você ajuda o servidor a continuar online, receber melhorias e ganhar novos sistemas para todos os jogadores.
                        ### 🛒 » Como comprar?
                        Selecione um VIP no menu abaixo para ver os detalhes e iniciar a compra.

                        -# <:iconswarning:1516640554576904202> Caso já possua um VIP, você também pode melhorar seu plano atual.""")
                .setImage(cfg.shopImageUrl())
                .setThumbnail(cfg.shopThumbnailUrl())
                .build();

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

    public static MessageCreateData tierDetail(VipConfig.VipTier tier) {
        return switch (tier.id()) {
            case "Vip" -> buildTierLegacy(C_VIP, "vip", "1516290472979595436", "VIP",
                    "Plano inicial para quem quer mais conforto, limites maiores e comandos exclusivos.",
                    "Teleporte sem tempo de espera\nReparo sem tempo de espera\nRecompensa diária exclusiva\nAcesso a comandos especiais",
                    tier);
            case "Elite" -> buildTierLegacy(C_ELITE, "elite", "1516290514847269014", "Elite",
                    "Plano avançado para jogadores que querem mais limites, recursos especiais e mais conforto.",
                    "Teleporte sem tempo de espera\nReparo sem tempo de espera\nSem desconexão por AFK\nMantém XP ao morrer\nRecompensa diária exclusiva",
                    tier);
            case "Ultra" -> buildTierLegacy(C_ULTRA, "ultra", "1516290555653390448", "Ultra",
                    "Plano superior com limites altos, recursos avançados e comandos especiais.",
                    "Sem limite de minions e profissões\nTeleporte sem tempo de espera\nSem desconexão por AFK\nMantém XP ao morrer\nAcesso ao comando /fly",
                    tier);
            default -> genericDetail(tier);
        };
    }

    private static MessageCreateData buildTierLegacy(int color, String emojiName, String emojiId,
                                                      String name, String desc, String highlights,
                                                      VipConfig.VipTier tier) {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(color)
                .setDescription("## — <:" + emojiName + ":" + emojiId + "> » " + name + "\n"
                        + "-# " + desc + "\n\n### ✨ » Destaques\n" + highlights)
                .build();
        return new MessageCreateBuilder()
                .addEmbeds(embed)
                .addComponents(ActionRow.of(buyButtonLegacy(tier.id())))
                .build();
    }

    private static MessageCreateData genericDetail(VipConfig.VipTier tier) {
        StringBuilder perks = new StringBuilder();
        for (String p : tier.perks()) perks.append("• ").append(p).append('\n');
        MessageEmbed embed = new EmbedBuilder()
                .setColor(C_SHOP)
                .setTitle("⭐ VIP " + tier.displayName())
                .setDescription(tier.description())
                .addField("💰 Preço", tier.price() + " MobCoins", true)
                .addField("🎁 Benefícios", perks.isEmpty() ? "Sem descrição." : perks.toString(), false)
                .setFooter("Haiz Network • VIP Shop")
                .setTimestamp(Instant.now())
                .build();
        return new MessageCreateBuilder()
                .addEmbeds(embed)
                .addComponents(ActionRow.of(buyButtonLegacy(tier.id())))
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  UTILITÁRIOS
    // ══════════════════════════════════════════════════════════════════════

    private static DataObject buyButton(String tierId) {
        return DataObject.empty()
                .put("type", 2)
                .put("style", 2) // Secondary
                .put("custom_id", "haiz:vip:buy:" + tierId)
                .put("label", "Comprar")
                .put("emoji", DataObject.empty()
                        .put("id", "1517986569380691993")
                        .put("name", "Carrinho"));
    }

    private static Button buyButtonLegacy(String tierId) {
        return Button.of(ButtonStyle.SECONDARY, "haiz:vip:buy:" + tierId, "Comprar", EMOJI_CART);
    }

    private static DataObject simpleV2(int color, String title, String description) {
        V2MessageBuilder builder = V2MessageBuilder.create();
        builder.addContainer(color, children -> {
            children.add(DataObject.empty().put("type", 10).put("content",
                    "## " + title + "\n" + description));
        });
        return builder.build();
    }

    private static int tierColor(String tierId) {
        return switch (tierId) {
            case "Vip"    -> C_VIP;
            case "Elite"  -> C_ELITE;
            case "Ultra"  -> C_ULTRA;
            case "Famoso" -> C_FAMOSO;
            case "Midia"  -> C_MIDIA;
            default       -> C_SHOP;
        };
    }
}
