package com.haiz.servercore.vip;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class VipEmbedFactory {

    // Cores exatas dos embeds originais
    private static final Color COLOR_VIP    = new Color(0xF8A016);
    private static final Color COLOR_ELITE  = new Color(0xED595A);
    private static final Color COLOR_ULTRA  = new Color(0x803EE2);
    private static final Color COLOR_FAMOSO = new Color(0x0E43ED);
    private static final Color COLOR_MIDIA  = new Color(0x0EB66A);
    private static final Color COLOR_SHOP   = new Color(0xFFAC30);
    private static final Color COLOR_GREEN  = new Color(0x2ECC71);
    private static final Color COLOR_RED    = new Color(0xE74C3C);
    private static final Color COLOR_BLUE   = new Color(0x5865F2);
    private static final Color COLOR_PURPLE = new Color(0x9B59B6);

    // Emoji do carrinho (compartilhado em todos os botões Comprar)
    private static final Emoji EMOJI_CART = Emoji.fromCustom("Carrinho", 1517986569380691993L, false);

    private VipEmbedFactory() {}

    // ── Mensagem principal da loja ────────────────────────────────────────────

    public static MessageCreateData shopMessage(VipConfig cfg) {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(COLOR_SHOP)
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

    // ── Embeds de detalhe por VIP (fiéis aos JSONs originais) ────────────────

    public static MessageCreateData vipDetail() {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(COLOR_VIP)
                .setDescription("""
                        ## — <:vip:1516290472979595436> » VIP
                        -# Plano inicial para quem quer mais conforto, limites maiores e comandos exclusivos.
                        ### ✨ » Destaques
                        • Teleporte sem tempo de espera
                        • Reparo sem tempo de espera
                        • Recompensa diária exclusiva
                        • Acesso a comandos especiais""")
                .addField("<a:XP:1490100752167862324> » Limites", """
                        • **1** Mascote
                        • **5** Profissões
                        • **50** Minions
                        • **20** Blocos de claim
                        • **60** Lojas de baú
                        • **40** `/pv`
                        • **30** `/pw`
                        • **20** Missões ativas
                        • **500** Leilões
                        • **70** Casas""", true)
                .addField("<a:pikaminediamonds:1493009193320316968> » Recursos", """
                        • Ignora espera de reparo
                        • Recompensa diária
                        • Benefícios exclusivos de VIP""", true)
                .addField("<:Console:1406969852765405266> » Comandos", """
                        • `/ps flag`
                        • `/anvil` `/nick` `/glow`
                        • `/feed` `/heal` `/craft`
                        • `/enderchest` `/kit`
                        • `/ignore` `/itemname` `/hat`""", false)
                .build();

        return new MessageCreateBuilder()
                .addEmbeds(embed)
                .addComponents(ActionRow.of(buyButton("Vip")))
                .build();
    }

    public static MessageCreateData eliteDetail() {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(COLOR_ELITE)
                .setDescription("""
                        ## — <:elite:1516290514847269014> » Elite
                        -# Plano avançado para jogadores que querem mais limites, recursos especiais e mais conforto no servidor.
                        ### ✨ » Destaques
                        • Teleporte sem tempo de espera
                        • Reparo sem tempo de espera
                        • Sem desconexão por AFK
                        • Mantém XP ao morrer
                        • Recompensa diária exclusiva""")
                .addField("<a:XP:1490100752167862324> » Limites", """
                        • **2** Mascotes
                        • **8** Profissões
                        • **100** Minions
                        • **30** Blocos de claim
                        • **70** Lojas de baú
                        • **50** `/pv`
                        • **40** `/pw`
                        • **25** Missões ativas
                        • **700** Leilões
                        • **100** Casas""", true)
                .addField("<a:pikaminediamonds:1493009193320316968> » Recursos", """
                        • Ignora desconexão por AFK
                        • Recompensa diária
                        • Mantém XP""", true)
                .addField("<:Console:1406969852765405266> » Comandos", """
                        • `/ps flag`
                        • `/anvil` `/nick` `/glow`
                        • `/feed` `/heal` `/craft`
                        • `/enderchest` `/kit`
                        • `/ignore` `/itemname` `/hat`""", false)
                .build();

        return new MessageCreateBuilder()
                .addEmbeds(embed)
                .addComponents(ActionRow.of(buyButton("Elite")))
                .build();
    }

    public static MessageCreateData ultraDetail() {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(COLOR_ULTRA)
                .setDescription("""
                        ## — <:ultra:1516290555653390448> » Ultra
                        -# Plano superior para jogadores que querem limites altos, recursos avançados e comandos especiais.
                        ### ✨ » Destaques
                        • Sem limite de minions
                        • Sem limite de profissões
                        • Teleporte sem tempo de espera
                        • Reparo sem tempo de espera
                        • Sem desconexão por AFK
                        • Mantém XP ao morrer
                        • Acesso ao comando `/fly`""")
                .addField("<a:XP:1490100752167862324> » Limites", """
                        • **4** Mascotes
                        • **50** Blocos de claim
                        • **100** Lojas de baú
                        • **60** `/pv`
                        • **50** `/pw`
                        • **30** Missões ativas
                        • **1000** Leilões
                        • **1000** Casas""", true)
                .addField("<a:pikaminediamonds:1493009193320316968> » Recursos", """
                        • Ignora desconexão por AFK
                        • Recompensa diária
                        • Mantém XP""", true)
                .addField("<:Console:1406969852765405266> » Comandos", """
                        • `/ps flag`
                        • `/ptime`
                        • `/anvil` `/nick` `/glow`
                        • `/feed` `/heal` `/craft`
                        • `/enderchest` `/kit`
                        • `/ignore` `/itemname` `/fly` `/hat`""", false)
                .build();

        return new MessageCreateBuilder()
                .addEmbeds(embed)
                .addComponents(ActionRow.of(buyButton("Ultra")))
                .build();
    }

    public static MessageCreateData famosoDetail() {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(COLOR_FAMOSO)
                .setDescription("""
                        ## — <:famoso:1516639945358577756> » Famoso
                        -# Cargo especial para jogadores reconhecidos dentro do servidor.

                        ✨ **Inclui todos os benefícios do Ultra**
                        🌟 **Destaque especial como Famoso**
                        💎 **Ideal para quem quer mais reconhecimento no servidor**""")
                .build();

        return new MessageCreateBuilder()
                .addEmbeds(embed)
                .addComponents(ActionRow.of(buyButton("Famoso")))
                .build();
    }

    public static MessageCreateData midiaDetail() {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(COLOR_MIDIA)
                .setDescription("""
                        ## — <:midia:1516639500317626469> » Mídia
                        -# Cargo especial para criadores de conteúdo e divulgadores do servidor.
                        ### ✨ » Benefícios
                        • Todos os benefícios do **Elite**
                        • Cargo exclusivo de **Mídia**
                        • Reconhecimento como criador de conteúdo
                        • Ideal para quem grava, posta ou divulga o servidor""")
                .addField("<a:pikaminediamonds:1493009193320316968> » Inclui", """
                        • Benefícios do plano **Elite**
                        • Identificação especial no servidor""", false)
                .build();

        return new MessageCreateBuilder()
                .addEmbeds(embed)
                .addComponents(ActionRow.of(buyButton("Midia")))
                .build();
    }

    // ── Roteador: retorna o embed correto pelo ID do tier ────────────────────

    public static MessageCreateData tierDetail(VipConfig.VipTier tier) {
        return switch (tier.id()) {
            case "Vip"    -> vipDetail();
            case "Elite"  -> eliteDetail();
            case "Ultra"  -> ultraDetail();
            case "Famoso" -> famosoDetail();
            case "Midia"  -> midiaDetail();
            default       -> genericDetail(tier); // fallback para tiers customizados
        };
    }

    // ── Fallback genérico (para tiers adicionados futuramente no config) ─────

    private static MessageCreateData genericDetail(VipConfig.VipTier tier) {
        StringBuilder perks = new StringBuilder();
        for (String p : tier.perks()) perks.append("• ").append(p).append('\n');

        MessageEmbed embed = new EmbedBuilder()
                .setColor(COLOR_SHOP)
                .setTitle("⭐ VIP " + tier.displayName())
                .setDescription(tier.description())
                .addField("💰 Preço", tier.price() + " MobCoins", true)
                .addField("🎁 Benefícios", perks.isEmpty() ? "Sem descrição." : perks.toString(), false)
                .setFooter("Haiz Network • VIP Shop")
                .setTimestamp(Instant.now())
                .build();

        return new MessageCreateBuilder()
                .addEmbeds(embed)
                .addComponents(ActionRow.of(buyButton(tier.id())))
                .build();
    }

    // ── Confirmação de compra ─────────────────────────────────────────────────

    public static MessageCreateData purchaseConfirm(VipConfig.VipTier tier, String mcName, double balance) {
        String balanceStr = balance < 0 ? "Desconhecido" : String.format("%.0f MC", balance);
        MessageEmbed embed = new EmbedBuilder()
                .setColor(COLOR_SHOP)
                .setTitle("🛒 Confirmar Compra — " + tier.displayName())
                .setDescription("Você está prestes a comprar o **VIP " + tier.displayName() + "**.\nEsta ação **não pode ser desfeita**.")
                .addField("👤 Conta Minecraft", "`" + mcName + "`", true)
                .addField("💰 Preço", tier.price() + " MobCoins", true)
                .addField("💳 Seu saldo", balanceStr, true)
                .setFooter("Haiz Network • VIP Shop")
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
                .setColor(COLOR_BLUE)
                .setTitle("🔗 Vincule sua conta Minecraft")
                .setDescription("""
                        Para comprar um VIP, você precisa vincular sua conta do Discord ao Minecraft.

                        **Como vincular:**
                        1️⃣ Esteja **online** no servidor Minecraft
                        2️⃣ Use o comando abaixo com seu nick exato:
                        ```
                        /verificar <seu nick>
                        ```
                        3️⃣ Você receberá um **código** no chat do Minecraft
                        4️⃣ Digite `/linkar <código>` no Minecraft para confirmar""")
                .setFooter("Haiz Network • Verificação")
                .setTimestamp(Instant.now())
                .build();

        return new MessageCreateBuilder().addEmbeds(embed).build();
    }

    // ── Resultados de operações ───────────────────────────────────────────────

    public static MessageCreateData playerOffline(String nick) {
        return simple(COLOR_RED, "❌ Jogador offline",
                "**" + nick + "** não está online no servidor no momento.\nEntre no servidor e tente novamente.");
    }

    public static MessageCreateData codeSent(String nick, int expirySeconds) {
        return simple(COLOR_GREEN, "✅ Código enviado!",
                "Um código de verificação foi enviado para **" + nick + "** no chat do Minecraft.\n"
                        + "Digite `/linkar <código>` no servidor.\nExpira em **" + expirySeconds + " segundos**.");
    }

    public static MessageCreateData linkSuccess(String nick) {
        return simple(COLOR_GREEN, "🎉 Conta vinculada!",
                "Sua conta Discord foi vinculada com sucesso a **" + nick + "**!\nAgora você pode comprar VIPs.");
    }

    public static MessageCreateData purchaseSuccess(VipConfig.VipTier tier) {
        return simple(COLOR_GREEN, "🎉 Compra realizada!",
                "Você adquiriu o **VIP " + tier.displayName() + "** com sucesso!\n"
                        + "Suas vantagens já estão ativas no servidor. Obrigado pelo apoio! ❤️");
    }

    public static MessageCreateData insufficientFunds(VipConfig.VipTier tier, double balance) {
        String balStr = balance < 0 ? "desconhecido" : String.format("%.0f", balance);
        return simple(COLOR_RED, "❌ Saldo insuficiente",
                "Você precisa de **" + tier.price() + " MobCoins** para este VIP.\n"
                        + "Seu saldo atual: **" + balStr + " MobCoins**.");
    }

    public static MessageCreateData purchaseCancelled() {
        return simple(COLOR_RED, "❌ Compra cancelada",
                "Você cancelou a compra. Nenhuma cobrança foi realizada.");
    }

    public static MessageCreateData purchaseExpired() {
        return simple(COLOR_RED, "⏱️ Tempo esgotado",
                "A sessão de compra expirou. Selecione o VIP novamente para reiniciar.");
    }

    public static MessageCreateData error(String msg) {
        return simple(COLOR_RED, "⚠️ Erro", msg);
    }

    // ── Log de compra (canal de logs) ─────────────────────────────────────────

    public static MessageEmbed purchaseLog(String discordTag, String mcName,
                                           VipConfig.VipTier tier, long price) {
        return new EmbedBuilder()
                .setColor(COLOR_PURPLE)
                .setTitle("🛍️ Nova compra de VIP")
                .addField("Discord", discordTag, true)
                .addField("Minecraft", "`" + mcName + "`", true)
                .addField("VIP", tier.displayName(), true)
                .addField("Preço", price + " MobCoins", true)
                .setTimestamp(Instant.now())
                .setFooter("Haiz Network • VIP Shop")
                .build();
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    /** Botão "Comprar" padrão com emoji Carrinho, custom_id = comprar_<tier em minúsculo> */
    private static Button buyButton(String tierId) {
        return Button.of(
                ButtonStyle.SECONDARY,
                "haiz:vip:buy:" + tierId,
                "Comprar",
                EMOJI_CART
        );
    }

    private static MessageCreateData simple(Color color, String title, String desc) {
        return new MessageCreateBuilder()
                .addEmbeds(new EmbedBuilder()
                        .setColor(color).setTitle(title).setDescription(desc)
                        .setTimestamp(Instant.now()).build())
                .build();
    }
}