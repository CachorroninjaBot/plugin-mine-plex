package com.haiz.servercore.vip;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Persiste vínculos Discord ↔ Minecraft e códigos de verificação pendentes.
 * Reutiliza a conexão SQLite principal do plugin.
 */
public final class LinkStorage {

    private final JavaPlugin plugin;
    private final com.haiz.servercore.storage.SQLiteDatabase db;

    public LinkStorage(JavaPlugin plugin, com.haiz.servercore.storage.SQLiteDatabase db) {
        this.plugin = plugin;
        this.db = db;
        createTables();
    }

    // ── DDL ─────────────────────────────────────────────────────────────────

    private void createTables() {
        exec("""
                CREATE TABLE IF NOT EXISTS discord_links (
                  discord_id TEXT PRIMARY KEY,
                  uuid       TEXT NOT NULL UNIQUE,
                  mc_name    TEXT NOT NULL,
                  linked_at  INTEGER NOT NULL
                )
                """);
        exec("CREATE INDEX IF NOT EXISTS idx_links_uuid ON discord_links(uuid)");
        exec("""
                CREATE TABLE IF NOT EXISTS link_codes (
                  code        TEXT PRIMARY KEY,
                  uuid        TEXT NOT NULL,
                  mc_name     TEXT NOT NULL,
                  discord_id  TEXT NOT NULL,
                  expires_at  INTEGER NOT NULL
                )
                """);
        exec("CREATE INDEX IF NOT EXISTS idx_codes_uuid ON link_codes(uuid)");
        exec("CREATE INDEX IF NOT EXISTS idx_codes_expires ON link_codes(expires_at)");
        exec("""
                CREATE TABLE IF NOT EXISTS vip_purchases (
                  id          INTEGER PRIMARY KEY AUTOINCREMENT,
                  discord_id  TEXT NOT NULL,
                  uuid        TEXT NOT NULL,
                  mc_name     TEXT NOT NULL,
                  vip_tier    TEXT NOT NULL,
                  price       INTEGER NOT NULL,
                  purchased_at INTEGER NOT NULL
                )
                """);
        addColumn("discord_links", "mc_name TEXT NOT NULL DEFAULT ''");
    }

    // ── Link codes ───────────────────────────────────────────────────────────

    public void savePendingCode(String code, UUID uuid, String mcName, String discordId, long expiresAt) {
        exec("DELETE FROM link_codes WHERE uuid=? OR discord_id=?", uuid.toString(), discordId);
        exec("INSERT INTO link_codes(code,uuid,mc_name,discord_id,expires_at) VALUES(?,?,?,?,?)",
                code, uuid.toString(), mcName, discordId, expiresAt);
    }

    public record PendingCode(String code, UUID uuid, String mcName, String discordId, long expiresAt) {}

    public Optional<PendingCode> findCode(String code) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT * FROM link_codes WHERE code=? AND expires_at>?")) {
            ps.setString(1, code);
            ps.setLong(2, now());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new PendingCode(
                        rs.getString("code"),
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("mc_name"),
                        rs.getString("discord_id"),
                        rs.getLong("expires_at")));
            }
        } catch (SQLException e) { warn(e); }
        return Optional.empty();
    }

    public void deleteCode(String code) {
        exec("DELETE FROM link_codes WHERE code=?", code);
    }

    public void deleteCodeByUuid(UUID uuid) {
        exec("DELETE FROM link_codes WHERE uuid=?", uuid.toString());
    }

    // ── Pending code lookup by UUID (for /linkar command) ───────────────────

    public Optional<PendingCode> findCodeByUuid(UUID uuid) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT * FROM link_codes WHERE uuid=? AND expires_at>?")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, now());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new PendingCode(
                        rs.getString("code"),
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("mc_name"),
                        rs.getString("discord_id"),
                        rs.getLong("expires_at")));
            }
        } catch (SQLException e) { warn(e); }
        return Optional.empty();
    }

    // ── Discord links ────────────────────────────────────────────────────────

    public void saveLink(String discordId, UUID uuid, String mcName) {
        exec("""
                INSERT INTO discord_links(discord_id,uuid,mc_name,linked_at)
                VALUES(?,?,?,?)
                ON CONFLICT(discord_id) DO UPDATE SET uuid=excluded.uuid,mc_name=excluded.mc_name,linked_at=excluded.linked_at
                """, discordId, uuid.toString(), mcName, now());
    }

    public Optional<String> discordIdByUuid(UUID uuid) {
        return queryString("SELECT discord_id FROM discord_links WHERE uuid=?", uuid.toString());
    }

    public Optional<UUID> uuidByDiscordId(String discordId) {
        return queryString("SELECT uuid FROM discord_links WHERE discord_id=?", discordId)
                .map(UUID::fromString);
    }

    public Optional<String> mcNameByDiscordId(String discordId) {
        return queryString("SELECT mc_name FROM discord_links WHERE discord_id=?", discordId);
    }

    public boolean isLinked(String discordId) {
        return uuidByDiscordId(discordId).isPresent();
    }

    // ── Purchase log ─────────────────────────────────────────────────────────

    public void logPurchase(String discordId, UUID uuid, String mcName, String tier, long price) {
        exec("INSERT INTO vip_purchases(discord_id,uuid,mc_name,vip_tier,price,purchased_at) VALUES(?,?,?,?,?,?)",
                discordId, uuid.toString(), mcName, tier, price, now());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Optional<String> queryString(String sql, String param) {
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getString(1));
            }
        } catch (SQLException e) { warn(e); }
        return Optional.empty();
    }

    private void exec(String sql, Object... params) {
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object p = params[i];
                if (p instanceof String s) ps.setString(i + 1, s);
                else if (p instanceof Long l) ps.setLong(i + 1, l);
                else if (p instanceof Integer v) ps.setInt(i + 1, v);
                else ps.setObject(i + 1, p);
            }
            ps.executeUpdate();
        } catch (SQLException e) { warn(e); }
    }

    private void addColumn(String table, String def) {
        try (Statement st = db.connection().createStatement()) {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + def);
        } catch (SQLException e) {
            if (e.getErrorCode() != 1) { // 1 = column already exists
                warn(e);
            }
        }
    }

    private void warn(SQLException e) {
        plugin.getLogger().warning("[VipShop] SQLite: " + e.getMessage());
    }

    private long now() { return com.haiz.servercore.utils.TimeUtils.nowSeconds(); }
}