package com.haiz.servercore.vip;

import com.haiz.servercore.HaizServerCore;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.command.PluginCommand;

/**
 * Módulo principal do sistema de loja de VIPs.
 * Inicializado em HaizServerCore.onEnable() após o DiscordBotManager.
 */
public final class VipModule {

    private final HaizServerCore plugin;
    private VipConfig vipConfig;
    private LinkStorage linkStorage;
    private LinkManager linkManager;
    private MobCoinsHook mobCoins;
    private PurchaseManager purchaseManager;
    private VipDiscordListener discordListener;
    private boolean running;

    public VipModule(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.vipConfig     = new VipConfig(plugin.getConfig());
        this.linkStorage   = new LinkStorage(plugin, plugin.database().sqliteDatabase());
        this.linkManager   = new LinkManager(plugin, linkStorage, vipConfig);
        this.mobCoins      = new MobCoinsHook(plugin);
        this.purchaseManager = new PurchaseManager(plugin, mobCoins, linkStorage, vipConfig);

        registerMinecraftCommands();
        registerDiscordListener();
        registerDiscordSlashCommand();

        running = true;
        plugin.getLogger().info("[VipShop] Módulo de loja de VIPs iniciado.");
    }

    public void stop() {
        if (discordListener != null && plugin.discord().jda() != null) {
            plugin.discord().jda().removeEventListener(discordListener);
        }
        running = false;
    }

    public void reload() {
        stop();
        start();
    }

    // ── Comando Minecraft: /haizcore sendvips ────────────────────────────────

    /**
     * Envia o embed de loja para o canal configurado.
     * Chamado por HaizCoreCommand quando o staff digita /haizcore sendvips.
     */
    public void sendShopMessage(org.bukkit.command.CommandSender sender) {
        if (!running) { sender.sendMessage("§cMódulo VIP não está ativo."); return; }
        if (!plugin.discord().isOnline()) {
            sender.sendMessage("§cDiscord offline. Não é possível enviar a mensagem.");
            return;
        }
        String channelId = vipConfig.vipShopChannelId();
        if (channelId.isBlank()) {
            sender.sendMessage("§cConfigura §fvip.discord.shop-channel-id §cno config.yml primeiro.");
            return;
        }
        TextChannel channel = plugin.discord().jda().getTextChannelById(channelId);
        if (channel == null) {
            sender.sendMessage("§cCanal §f" + channelId + " §cnão encontrado no Discord.");
            return;
        }
        channel.sendMessage(VipEmbedFactory.shopMessage(vipConfig)).queue(
                msg -> sender.sendMessage("§aMensagem de VIPs enviada com sucesso!"),
                err -> sender.sendMessage("§cFalha ao enviar: " + err.getMessage())
        );
    }

    // ── Internos ─────────────────────────────────────────────────────────────

    private void registerMinecraftCommands() {
        VipMinecraftCommands executor = new VipMinecraftCommands(this);
        PluginCommand linkar = plugin.getCommand("linkar");
        if (linkar != null) {
            linkar.setExecutor(executor);
            linkar.setTabCompleter(executor);
        }
    }

    private void registerDiscordListener() {
        if (!plugin.discord().isOnline()) return;
        discordListener = new VipDiscordListener(this);
        plugin.discord().jda().addEventListener(discordListener);
    }

    private void registerDiscordSlashCommand() {
        if (!plugin.discord().isOnline()) return;
        var jda   = plugin.discord().jda();
        String guildId = plugin.config().guildId();
        var cmd = Commands.slash("verificar", "Vincule sua conta Minecraft ao Discord")
                .addOption(OptionType.STRING, "nick", "Seu nick no servidor Minecraft", true);

        if (guildId != null && !guildId.isBlank()) {
            var guild = jda.getGuildById(guildId);
            if (guild != null) guild.upsertCommand(cmd).queue();
        } else {
            jda.upsertCommand(cmd).queue();
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public HaizServerCore plugin()          { return plugin; }
    public VipConfig vipConfig()            { return vipConfig; }
    public LinkManager linkManager()        { return linkManager; }
    public MobCoinsHook mobCoins()          { return mobCoins; }
    public PurchaseManager purchaseManager(){ return purchaseManager; }
    public boolean isRunning()              { return running; }
}