package com.haiz.servercore.vip;

import com.haiz.servercore.HaizServerCore;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

import java.util.List;

public final class VipModule {

    private final HaizServerCore plugin;
    private VipConfig vipConfig;
    private LinkStorage linkStorage;
    private LinkManager linkManager;
    private MobCoinsHook mobCoins;
    private VipStorage vipStorage;
    private PurchaseManager purchaseManager;
    private VipDiscordListener discordListener;
    private VipRenewalTask renewalTask;
    private AutoRepairManager autoRepairManager;
    private boolean running;

    public VipModule(HaizServerCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.vipConfig     = new VipConfig(plugin.getConfig());
        this.linkStorage   = new LinkStorage(plugin, plugin.sqliteDatabase());
        this.linkManager   = new LinkManager(plugin, linkStorage, vipConfig);
        this.mobCoins      = new MobCoinsHook(plugin);
        this.vipStorage    = new VipStorage(plugin, plugin.sqliteDatabase());
        this.purchaseManager = new PurchaseManager(plugin, mobCoins, linkStorage, vipConfig, vipStorage);

        this.autoRepairManager = new AutoRepairManager(plugin, vipStorage, vipConfig);

        registerMinecraftCommands();

        this.renewalTask = new VipRenewalTask(plugin, vipStorage, mobCoins, vipConfig, purchaseManager::invalidateVipCache);
        this.renewalTask.start();

        this.autoRepairManager.register();

        scheduleDiscordSetup();

        running = true;
        plugin.getLogger().info("[VipShop] Módulo de loja de VIPs iniciado.");
    }

    public void stop() {
        if (autoRepairManager != null) {
            autoRepairManager.unregister();
        }
        if (renewalTask != null) {
            renewalTask.stop();
        }
        if (discordListener != null && plugin.discord().jda() != null) {
            plugin.discord().jda().removeEventListener(discordListener);
        }
        autoRepairManager = null;
        discordListener = null;
        renewalTask = null;
        purchaseManager = null;
        linkManager = null;
        linkStorage = null;
        mobCoins = null;
        vipStorage = null;
        vipConfig = null;
        running = false;
    }

    public void reload() {
        stop();
        start();
    }

    private void scheduleDiscordSetup() {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (plugin.discord().isOnline()) {
                registerDiscordListener();
                registerDiscordSlashCommands();
                task.cancel();
            } else if (!running) {
                task.cancel();
            }
        }, 20L, 40L);
    }

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

        try {
            net.dv8tion.jda.api.utils.data.DataObject v2msg = VipEmbedFactory.shopMessageV2(vipConfig);
            V2Messenger.send(channel, v2msg).thenAccept(status -> {
                if (status >= 200 && status < 300) {
                    sender.sendMessage("§aMensagem de VIPs enviada com sucesso! (Components v2)");
                } else {
                    plugin.getLogger().warning("[VipShop] V2 retornou HTTP " + status + ", tentando legacy...");
                    channel.sendMessage(VipEmbedFactory.shopMessage(vipConfig)).queue(
                            msg -> sender.sendMessage("§aMensagem de VIPs enviada com sucesso! (legacy)"),
                            err -> sender.sendMessage("§cFalha ao enviar: " + err.getMessage())
                    );
                }
            }).exceptionally(err -> {
                plugin.getLogger().warning("[VipShop] Falha ao enviar V2, tentando legacy: " + err.getMessage());
                channel.sendMessage(VipEmbedFactory.shopMessage(vipConfig)).queue(
                        msg -> sender.sendMessage("§aMensagem de VIPs enviada com sucesso! (legacy)"),
                        err2 -> sender.sendMessage("§cFalha ao enviar: " + err2.getMessage())
                );
                return null;
            });
        } catch (Exception e) {
            plugin.getLogger().warning("[VipShop] Erro ao construir V2: " + e.getMessage());
            channel.sendMessage(VipEmbedFactory.shopMessage(vipConfig)).queue(
                    msg -> sender.sendMessage("§aMensagem de VIPs enviada com sucesso! (legacy)"),
                    err -> sender.sendMessage("§cFalha ao enviar: " + err.getMessage())
            );
        }
    }

    // ── Internos ─────────────────────────────────────────────────────────────

    private void registerMinecraftCommands() {
        VipMinecraftCommands executor = new VipMinecraftCommands(this);
        PluginCommand linkar = plugin.getCommand("linkar");
        if (linkar != null) {
            linkar.setExecutor(executor);
            linkar.setTabCompleter(executor);
        }

        VipSettingsCommand settingsExecutor = new VipSettingsCommand(this);
        PluginCommand vipconfig = plugin.getCommand("vipconfig");
        if (vipconfig != null) {
            vipconfig.setExecutor(settingsExecutor);
            vipconfig.setTabCompleter(settingsExecutor);
        }

        RepairCommand repairExecutor = new RepairCommand(autoRepairManager);
        PluginCommand repair = plugin.getCommand("repair");
        if (repair != null) {
            repair.setExecutor(repairExecutor);
            repair.setTabCompleter(repairExecutor);
        }
    }

    private void registerDiscordListener() {
        if (!plugin.discord().isOnline()) return;
        if (discordListener != null) {
            plugin.discord().jda().removeEventListener(discordListener);
        }
        discordListener = new VipDiscordListener(this);
        plugin.discord().jda().addEventListener(discordListener);
    }

    private void registerDiscordSlashCommands() {
        if (!plugin.discord().isOnline()) return;
        var jda = plugin.discord().jda();
        String guildId = plugin.config().guildId();

        var verificar = Commands.slash("verificar", "Vincule sua conta Minecraft ao Discord")
                .addOption(OptionType.STRING, "nick", "Seu nick no servidor Minecraft", true);

        var vipconfig = Commands.slash("vipconfig", "Veja as configurações do seu VIP");

        List<net.dv8tion.jda.api.interactions.commands.build.SlashCommandData> commands = List.of(verificar, vipconfig);

        if (guildId != null && !guildId.isBlank()) {
            Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands().addCommands(commands).queue(
                    success -> plugin.getLogger().info("[VipShop] Comandos registrados na guild: " + guild.getName() + " (/verificar, /vipconfig)"),
                    error -> plugin.getLogger().warning("[VipShop] Falha ao registrar comandos: " + error.getMessage())
                );
                return;
            }
            plugin.getLogger().warning("[VipShop] Guild ID " + guildId + " não encontrada. Registrando globalmente.");
        }

        jda.updateCommands().addCommands(commands).queue(
            success -> plugin.getLogger().info("[VipShop] Comandos registrados globalmente (/verificar, /vipconfig)"),
            error -> plugin.getLogger().warning("[VipShop] Falha ao registrar comandos: " + error.getMessage())
        );
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public HaizServerCore plugin()          { return plugin; }
    public VipConfig vipConfig()            { return vipConfig; }
    public LinkManager linkManager()        { return linkManager; }
    public MobCoinsHook mobCoins()          { return mobCoins; }
    public VipStorage vipStorage()          { return vipStorage; }
    public PurchaseManager purchaseManager(){ return purchaseManager; }
    public AutoRepairManager autoRepairManager() { return autoRepairManager; }
    public boolean isRunning()              { return running; }
}
