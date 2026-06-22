package com.haiz.servercore.vip;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

public final class VipStorage {

    private static final long VIP_DURATION_SECONDS = 30L * 24 * 60 * 60;

    private final JavaPlugin plugin;
    private final com.haiz.servercore.storage.SQLiteDatabase db;

    public VipStorage(JavaPlugin plugin, com.haiz.servercore.storage.SQLiteDatabase db) {
        this.plugin = plugin;
        this.db = db;
        createTables();
    }

    private void createTables() {
        exec("""
                CREATE TABLE IF NOT EXISTS vip_subscriptions (
                  uuid         TEXT PRIMARY KEY,
                  tier         TEXT NOT NULL,
                  purchased_at INTEGER NOT NULL,
                  expires_at   INTEGER NOT NULL
                )
                """);
        exec("CREATE INDEX IF NOT EXISTS idx_vip_expires ON vip_subscriptions(expires_at)");
        exec("""
                CREATE TABLE IF NOT EXISTS vip_settings (
                  uuid        TEXT PRIMARY KEY,
                  auto_renew  INTEGER NOT NULL DEFAULT 0,
                  auto_repair INTEGER NOT NULL DEFAULT 0
                )
                """);
    }

    // ── Subscriptions ─────────────────────────────────────────────────────

    public record VipSubscription(String tier, long purchasedAt, long expiresAt) {}

    public synchronized void saveSubscription(UUID uuid, String tier, long purchasedAt, long expiresAt) {
        exec("""
                INSERT INTO vip_subscriptions(uuid,tier,purchased_at,expires_at)
                VALUES(?,?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET tier=excluded.tier,
                  purchased_at=excluded.purchased_at, expires_at=excluded.expires_at
                """, uuid.toString(), tier, purchasedAt, expiresAt);
    }

    public synchronized Optional<VipSubscription> getActiveSubscription(UUID uuid) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT tier,purchased_at,expires_at FROM vip_subscriptions WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long expiresAt = rs.getLong("expires_at");
                    if (expiresAt > now()) {
                        return Optional.of(new VipSubscription(
                                rs.getString("tier"),
                                rs.getLong("purchased_at"),
                                expiresAt));
                    }
                }
            }
        } catch (SQLException e) { warn(e); }
        return Optional.empty();
    }

    public synchronized void renewSubscription(UUID uuid, long newExpiresAt) {
        exec("UPDATE vip_subscriptions SET expires_at=? WHERE uuid=?",
                newExpiresAt, uuid.toString());
    }

    public synchronized void removeSubscription(UUID uuid) {
        exec("DELETE FROM vip_subscriptions WHERE uuid=?", uuid.toString());
    }

    public synchronized java.util.List<ExpiringVip> getVipsExpiringBefore(long timestamp) {
        java.util.List<ExpiringVip> list = new java.util.ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT uuid,tier,expires_at FROM vip_subscriptions WHERE expires_at<=? AND expires_at>0")) {
            ps.setLong(1, timestamp);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ExpiringVip(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("tier"),
                            rs.getLong("expires_at")));
                }
            }
        } catch (SQLException e) { warn(e); }
        return list;
    }

    public record ExpiringVip(UUID uuid, String tier, long expiresAt) {}

    // ── Settings ──────────────────────────────────────────────────────────

    public synchronized boolean getAutoRenew(UUID uuid) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT auto_renew FROM vip_settings WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("auto_renew") == 1;
            }
        } catch (SQLException e) { warn(e); }
        return false;
    }

    public synchronized void setAutoRenew(UUID uuid, boolean enabled) {
        exec("""
                INSERT INTO vip_settings(uuid,auto_renew) VALUES(?,?)
                ON CONFLICT(uuid) DO UPDATE SET auto_renew=excluded.auto_renew
                """, uuid.toString(), enabled ? 1 : 0);
    }

    public synchronized boolean toggleAutoRenew(UUID uuid) {
        boolean current = getAutoRenew(uuid);
        setAutoRenew(uuid, !current);
        return !current;
    }

    // ── Auto Repair ──────────────────────────────────────────────────────

    public synchronized boolean getAutoRepair(UUID uuid) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT auto_repair FROM vip_settings WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("auto_repair") == 1;
            }
        } catch (SQLException e) { warn(e); }
        return false;
    }

    public synchronized void setAutoRepair(UUID uuid, boolean enabled) {
        exec("""
                INSERT INTO vip_settings(uuid,auto_repair) VALUES(?,?)
                ON CONFLICT(uuid) DO UPDATE SET auto_repair=excluded.auto_repair
                """, uuid.toString(), enabled ? 1 : 0);
    }

    public synchronized boolean toggleAutoRepair(UUID uuid) {
        boolean current = getAutoRepair(uuid);
        setAutoRepair(uuid, !current);
        return !current;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private synchronized void exec(String sql, Object... params) {
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

    private void warn(SQLException e) {
        plugin.getLogger().warning("[VipShop] VipStorage SQLite: " + e.getMessage());
    }

    private long now() { return System.currentTimeMillis() / 1000L; }

    public static long durationSeconds() { return VIP_DURATION_SECONDS; }
}
