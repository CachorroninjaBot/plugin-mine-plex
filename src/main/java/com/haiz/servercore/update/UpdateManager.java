package com.haiz.servercore.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Verifica releases no GitHub, baixa .jar automaticamente e aplica atualização.
 *
 * Fluxo:
 *  1. Verifica GitHub API periodically (configurável)
 *  2. Compara versão local vs remota
 *  3. Baixa .jar para pasta plugins/ com nome temporário
 *  4. Marca para aplicar no próximo reload/restart
 */
public final class UpdateManager {

    private final JavaPlugin plugin;
    private final Logger log;
    private final String githubOwner;
    private final String githubRepo;
    private final int checkIntervalMinutes;
    private int taskId = -1;
    private UpdateInfo latestUpdate = null;
    private boolean updateDownloaded = false;

    public record UpdateInfo(String version, String downloadUrl, String releaseNotes, String publishedAt) {}

    public UpdateManager(JavaPlugin plugin, String githubOwner, String githubRepo, int checkIntervalMinutes) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.githubOwner = githubOwner;
        this.githubRepo = githubRepo;
        this.checkIntervalMinutes = Math.max(5, checkIntervalMinutes);
    }

    public void start() {
        if (githubOwner == null || githubOwner.isBlank() || githubRepo == null || githubRepo.isBlank()) {
            log.info("[Updater] GitHub owner/repo não configurado. Auto-update desabilitado.");
            return;
        }
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkForUpdates,
                20L * 30, 20L * 60 * checkIntervalMinutes).getTaskId();
        log.info("[Updater] Verificação de atualizações iniciada (intervalo: " + checkIntervalMinutes + " min).");
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void checkForUpdates() {
        try {
            String apiUrl = "https://api.github.com/repos/" + githubOwner + "/" + githubRepo + "/releases/latest";
            String jsonResponse = httpGet(apiUrl);
            if (jsonResponse == null) return;

            JsonObject release = JsonParser.parseString(jsonResponse).getAsJsonObject();
            String tagName = release.has("tag_name") ? release.get("tag_name").getAsString() : "";
            String publishedAt = release.has("published_at") ? release.get("published_at").getAsString() : "";
            String body = release.has("body") ? release.get("body").getAsString() : "";

            String remoteVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;

            if (remoteVersion.isBlank()) return;

            if (!isNewerVersion(remoteVersion)) {
                log.info("[Updater] Versão atual (" + getCurrentVersion() + ") está atualizada.");
                return;
            }

            String downloadUrl = findJarAssetUrl(release);
            if (downloadUrl == null) {
                log.warning("[Updater] Release " + remoteVersion + " não contém um .jar.asset.");
                return;
            }

            latestUpdate = new UpdateInfo(remoteVersion, downloadUrl, body, publishedAt);
            log.info("[Updater] Nova versão disponível: " + remoteVersion + " (atual: " + getCurrentVersion() + ")");
        } catch (Exception e) {
            log.warning("[Updater] Erro ao verificar atualizações: " + e.getMessage());
        }
    }

    public CompletableFuture<Boolean> downloadUpdate(CommandSender reporter) {
        if (latestUpdate == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Path pluginsDir = plugin.getDataFolder().getParentFile().toPath();
                Path updateDir = pluginsDir.resolve("haizcore-updates");
                Files.createDirectories(updateDir);

                String jarName = "HaizServerCore-" + latestUpdate.version() + ".jar";
                Path target = updateDir.resolve(jarName);

                if (reporter != null) {
                    reporter.sendMessage("§b[Updater] §fBaixando versão " + latestUpdate.version() + "...");
                }
                log.info("[Updater] Baixando: " + latestUpdate.downloadUrl());

                httpDownload(latestUpdate.downloadUrl(), target);

                Path pendingMarker = pluginsDir.resolve("HaizServerCore.jar.pending");
                Files.writeString(pendingMarker, target.toAbsolutePath().toString());

                updateDownloaded = true;
                if (reporter != null) {
                    reporter.sendMessage("§a[Updater] §fVersão " + latestUpdate.version() + " baixada com sucesso!");
                    reporter.sendMessage("§e[Updater] §fA atualização será aplicada no próximo §6/haizcore reload§e.");
                }
                log.info("[Updater] Download concluído: " + target);
                return true;
            } catch (Exception e) {
                if (reporter != null) {
                    reporter.sendMessage("§c[Updater] §fFalha ao baixar: " + e.getMessage());
                }
                log.warning("[Updater] Falha no download: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Aplica update pendente se existir. Chamado no onEnable() antes de inicializar módulos.
     * O .jar não pode ser substituído enquanto o JVM está rodando (travado no Windows),
     * então usamos um script batch que executa a substituição após o servidor parar.
     */
    public boolean applyPendingUpdate() {
        try {
            Path pluginsDir = plugin.getDataFolder().getParentFile().toPath();
            Path pendingMarker = pluginsDir.resolve("HaizServerCore.jar.pending");

            if (!Files.exists(pendingMarker)) return false;

            Path newPath = Path.of(Files.readString(pendingMarker).trim());
            if (!Files.exists(newPath)) {
                Files.deleteIfExists(pendingMarker);
                return false;
            }

            Path currentJar = pluginsDir.resolve("HaizServerCore.jar");
            Path serverRoot = pluginsDir.getParent() != null ? pluginsDir.getParent() : Path.of(System.getProperty("user.dir"));

            Path updateScript = pluginsDir.resolve("haizcore-update.bat");
            String scriptContent = """
                    @echo off
                    echo ========================================
                    echo  HaizServerCore Auto-Update
                    echo ========================================
                    echo.
                    echo Aguardando servidor parar...
                    :check_loop
                    timeout /t 3 /nobreak >nul
                    tasklist /FI "IMAGENAME eq java.exe" 2>nul | find /I "java.exe" >nul
                    if %errorlevel%==0 goto check_loop
                    echo.
                    echo Servidor parado. Aplicando update...
                    timeout /t 1 /nobreak >nul
                    copy /Y "%s" "%s"
                    if %errorlevel%==0 (
                        echo Update aplicado com sucesso!
                        del /Q "%s"
                    ) else (
                        echo ERRO: Falha ao copiar arquivo.
                    )
                    echo.
                    echo ========================================
                    echo  Atualizacao concluida!
                    echo  Reinicie o servidor manualmente.
                    echo ========================================
                    pause
                    """.formatted(
                    newPath.toAbsolutePath(), currentJar.toAbsolutePath(),
                    pendingMarker.toAbsolutePath());
            Files.writeString(updateScript, scriptContent);

            log.info("[Updater] Script de update criado: " + updateScript);
            log.info("[Updater] Execute o script APOS o servidor parar, ou reinicie manualmente.");

            Runtime.getRuntime().exec("cmd /c start \"\" \"" + updateScript.toAbsolutePath() + "\"");

            return true;
        } catch (Exception e) {
            log.warning("[Updater] Falha ao aplicar update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Merge do config.yml: adiciona novas chaves do default sem substituir valores existentes do usuário.
     * Chamado no onEnable() para garantir que o config.yml tenha todas as chaves da versão atual.
     */
    public void mergeConfig() {
        try {
            plugin.saveDefaultConfig();
            plugin.reloadConfig();

            java.io.InputStream defaultStream = plugin.getResource("config.yml");
            if (defaultStream == null) return;

            org.bukkit.configuration.file.FileConfiguration defaultConfig =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                            new java.io.InputStreamReader(defaultStream, java.nio.charset.StandardCharsets.UTF_8));

            org.bukkit.configuration.file.FileConfiguration currentConfig = plugin.getConfig();
            boolean changed = false;

            for (String key : defaultConfig.getKeys(true)) {
                if (!currentConfig.contains(key)) {
                    currentConfig.set(key, defaultConfig.get(key));
                    changed = true;
                    log.info("[Updater] Config: chave adicionada: " + key);
                }
            }

            if (changed) {
                plugin.saveConfig();
                plugin.reloadConfig();
                log.info("[Updater] config.yml atualizado com novas chaves.");
            }
        } catch (Exception e) {
            log.warning("[Updater] Falha ao merge config.yml: " + e.getMessage());
        }
    }

    public boolean isUpdateAvailable() {
        return latestUpdate != null;
    }

    public boolean isUpdateDownloaded() {
        return updateDownloaded;
    }

    public UpdateInfo getLatestUpdate() {
        return latestUpdate;
    }

    public String getCurrentVersion() {
        return plugin.getDescription().getVersion();
    }

    private boolean isNewerVersion(String remoteVersion) {
        String local = getCurrentVersion();
        String[] localParts = local.split("\\.");
        String[] remoteParts = remoteVersion.split("\\.");

        int maxLen = Math.max(localParts.length, remoteParts.length);
        for (int i = 0; i < maxLen; i++) {
            int localNum = i < localParts.length ? parseVersionPart(localParts[i]) : 0;
            int remoteNum = i < remoteParts.length ? parseVersionPart(remoteParts[i]) : 0;
            if (remoteNum > localNum) return true;
            if (remoteNum < localNum) return false;
        }
        return false;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String findJarAssetUrl(JsonObject release) {
        if (!release.has("assets")) return null;
        JsonArray assets = release.getAsJsonArray("assets");
        for (JsonElement elem : assets) {
            JsonObject asset = elem.getAsJsonObject();
            String name = asset.has("name") ? asset.get("name").getAsString() : "";
            if (name.endsWith(".jar")) {
                return asset.has("browser_download_url") ? asset.get("browser_download_url").getAsString() : null;
            }
        }
        return null;
    }

    private String httpGet(String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "HaizServerCore-Updater/" + getCurrentVersion());
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);

        if (conn.getResponseCode() != 200) {
            conn.disconnect();
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private void httpDownload(String urlString, Path target) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "HaizServerCore-Updater/" + getCurrentVersion());
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        if (conn.getResponseCode() != 200) {
            throw new IOException("HTTP " + conn.getResponseCode());
        }

        Files.copy(conn.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        conn.disconnect();
    }
}
