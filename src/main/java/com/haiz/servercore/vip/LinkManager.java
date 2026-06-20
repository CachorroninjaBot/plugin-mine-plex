package com.haiz.servercore.vip;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Gerencia o fluxo de verificação Discord ↔ Minecraft.
 *
 * Fluxo:
 *  1. Usuário seleciona VIP no Discord → bot detecta que não está vinculado
 *  2. Bot instrui: "Use /verificar <nick> aqui no Discord"
 *  3. Usuário digita /verificar <nick> → LinkManager.initiateLink() salva código pendente
 *  4. Plugin envia código no chat do Minecraft via Bukkit scheduler
 *  5. Usuário digita o código no Minecraft → LinkManager.confirmCode() finaliza o vínculo
 */
public final class LinkManager {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem caracteres ambíguos
    private static final SecureRandom RNG = new SecureRandom();

    private final JavaPlugin plugin;
    private final LinkStorage storage;
    private final VipConfig vipConfig;

    public LinkManager(JavaPlugin plugin, LinkStorage storage, VipConfig vipConfig) {
        this.plugin = plugin;
        this.storage = storage;
        this.vipConfig = vipConfig;
    }

    // ── Passo 3: Discord solicita link ───────────────────────────────────────

    /**
     * Chamado quando o usuário digita /verificar <nick> no Discord.
     * Gera código e agenda envio no chat do Minecraft.
     *
     * @return código gerado, ou empty se o nick não estiver online
     */
    public Optional<String> initiateLink(String discordId, String mcName) {
        Player player = Bukkit.getPlayerExact(mcName);
        if (player == null) return Optional.empty();

        String code = generateCode();
        long expiresAt = now() + vipConfig.linkCodeExpirySeconds();
        storage.savePendingCode(code, player.getUniqueId(), player.getName(), discordId, expiresAt);

        // Envio no thread principal do Bukkit
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayerExact(mcName);
            if (p == null) return;
            p.sendMessage("");
            p.sendMessage("§8[§bHaizCore§8] §eVerificação Discord");
            p.sendMessage("§7Um usuário do Discord quer vincular sua conta.");
            p.sendMessage("§7Seu código: §b§l" + code);
            p.sendMessage("§7Digite §f/linkar " + code + " §7para confirmar.");
            p.sendMessage("§7Expira em §f" + vipConfig.linkCodeExpirySeconds() + "s§7.");
            p.sendMessage("");
        });

        return Optional.of(code);
    }

    // ── Passo 5: Jogador confirma no Minecraft ───────────────────────────────

    public enum ConfirmResult { SUCCESS, INVALID_CODE, EXPIRED }

    public ConfirmResult confirmCode(Player player, String code) {
        Optional<LinkStorage.PendingCode> opt = storage.findCode(code.toUpperCase());
        if (opt.isEmpty()) return ConfirmResult.INVALID_CODE;

        LinkStorage.PendingCode pending = opt.get();
        if (!pending.uuid().equals(player.getUniqueId())) return ConfirmResult.INVALID_CODE;
        if (pending.expiresAt() < now()) return ConfirmResult.EXPIRED;

        storage.saveLink(pending.discordId(), player.getUniqueId(), player.getName());
        storage.deleteCode(code.toUpperCase());
        return ConfirmResult.SUCCESS;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public boolean isLinked(String discordId) {
        return storage.isLinked(discordId);
    }

    public Optional<UUID> uuidByDiscordId(String discordId) {
        return storage.uuidByDiscordId(discordId);
    }

    public Optional<String> mcNameByDiscordId(String discordId) {
        return storage.mcNameByDiscordId(discordId);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(CHARS.charAt(RNG.nextInt(CHARS.length())));
        return sb.toString();
    }

    private long now() { return System.currentTimeMillis() / 1000L; }
}