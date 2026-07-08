package com.haiz.servercore.website;

import com.haiz.servercore.storage.SQLiteDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class StoreStorage {

    private final SQLiteDatabase db;

    public StoreStorage(SQLiteDatabase db) {
        this.db = db;
        createTables();
    }

    private void createTables() {
        exec("""
            CREATE TABLE IF NOT EXISTS store_purchases (
                id TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                player_uuid TEXT,
                item_id TEXT NOT NULL,
                item_name TEXT NOT NULL,
                price REAL NOT NULL,
                pix_txid TEXT,
                status TEXT NOT NULL DEFAULT 'pending',
                created_at INTEGER NOT NULL,
                paid_at INTEGER,
                delivered_at INTEGER
            )
        """);
        exec("CREATE INDEX IF NOT EXISTS idx_store_purchases_player ON store_purchases(player_name)");
        exec("CREATE INDEX IF NOT EXISTS idx_store_purchases_status ON store_purchases(status)");

        exec("""
            CREATE TABLE IF NOT EXISTS store_mobcoins_purchases (
                id TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                player_uuid TEXT,
                item_id TEXT NOT NULL,
                item_name TEXT NOT NULL,
                cost INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'pending',
                created_at INTEGER NOT NULL,
                delivered_at INTEGER
            )
        """);
        exec("CREATE INDEX IF NOT EXISTS idx_mc_purchases_player ON store_mobcoins_purchases(player_name)");
    }

    public void createPurchase(String id, String playerName, String playerUuid, String itemId, String itemName, double price, String txid) {
        exec("INSERT INTO store_purchases (id, player_name, player_uuid, item_id, item_name, price, pix_txid, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'pending', ?)",
            id, playerName, playerUuid, itemId, itemName, price, txid, System.currentTimeMillis() / 1000);
    }

    public Map<String, Object> getPurchase(String id) {
        return queryOne("SELECT * FROM store_purchases WHERE id = ?", id);
    }

    public void updatePurchaseStatus(String id, String status) {
        if ("paid".equals(status)) {
            exec("UPDATE store_purchases SET status = ?, paid_at = ? WHERE id = ?", status, System.currentTimeMillis() / 1000, id);
        } else if ("delivered".equals(status)) {
            exec("UPDATE store_purchases SET status = ?, delivered_at = ? WHERE id = ?", status, System.currentTimeMillis() / 1000, id);
        } else {
            exec("UPDATE store_purchases SET status = ? WHERE id = ?", status, id);
        }
    }

    public void createMobCoinsPurchase(String id, String playerName, String playerUuid, String itemId, String itemName, int cost) {
        exec("INSERT INTO store_mobcoins_purchases (id, player_name, player_uuid, item_id, item_name, cost, status, created_at) VALUES (?, ?, ?, ?, ?, ?, 'pending', ?)",
            id, playerName, playerUuid, itemId, itemName, cost, System.currentTimeMillis() / 1000);
    }

    public void updateMobCoinsPurchaseStatus(String id, String status) {
        if ("delivered".equals(status)) {
            exec("UPDATE store_mobcoins_purchases SET status = ?, delivered_at = ? WHERE id = ?", status, System.currentTimeMillis() / 1000, id);
        } else {
            exec("UPDATE store_mobcoins_purchases SET status = ? WHERE id = ?", status, id);
        }
    }

    public List<Map<String, Object>> getPlayerPurchases(String playerName) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT * FROM store_purchases WHERE player_name = ? ORDER BY created_at DESC LIMIT 20")) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger("StoreStorage").log(Level.WARNING, "Query error: " + e.getMessage());
        }
        return result;
    }

    private Map<String, Object> queryOne(String sql, Object... params) {
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rowToMap(rs);
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger("StoreStorage").log(Level.WARNING, "Query error: " + e.getMessage());
        }
        return null;
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        var meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            map.put(meta.getColumnName(i), rs.getObject(i));
        }
        return map;
    }

    private void exec(String sql, Object... params) {
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger("StoreStorage").log(Level.WARNING, "Exec error: " + e.getMessage());
        }
    }
}
