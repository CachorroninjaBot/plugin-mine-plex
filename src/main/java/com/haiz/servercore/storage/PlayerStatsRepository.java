package com.haiz.servercore.storage;

import com.haiz.servercore.metrics.PlayerStats;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PlayerStatsRepository {
    private final JavaPlugin plugin;
    private final SQLiteDatabase database;

    public PlayerStatsRepository(JavaPlugin plugin, SQLiteDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    public synchronized void upsertJoin(Player player, long nowSeconds) {
        upsertJoin(player.getUniqueId(), player.getName(), nowSeconds);
    }

    public synchronized void upsertJoin(UUID uuid, String name, long nowSeconds) {
        execute("""
                INSERT INTO players(uuid, name, first_join, last_join, total_sessions)
                VALUES (?, ?, ?, ?, 1)
                ON CONFLICT(uuid) DO UPDATE SET
                  name=excluded.name,
                  last_join=excluded.last_join,
                  total_sessions=players.total_sessions + 1
                """, statement -> {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setLong(3, nowSeconds);
            statement.setLong(4, nowSeconds);
        });
    }

    public synchronized void finishSession(UUID uuid, long joinTime, long quitTime, long durationSeconds, long afkSeconds) {
        execute("""
                INSERT INTO sessions(uuid, join_time, quit_time, duration_seconds, afk_seconds)
                VALUES (?, ?, ?, ?, ?)
                """, statement -> {
            statement.setString(1, uuid.toString());
            statement.setLong(2, joinTime);
            statement.setLong(3, quitTime);
            statement.setLong(4, durationSeconds);
            statement.setLong(5, afkSeconds);
        });
        execute("""
                UPDATE players
                SET last_quit=?, total_playtime_seconds=total_playtime_seconds + ?
                WHERE uuid=?
                """, statement -> {
            statement.setLong(1, quitTime);
            statement.setLong(2, Math.max(0, durationSeconds - afkSeconds));
            statement.setString(3, uuid.toString());
        });
    }

    public synchronized void logCommand(String executor, String executorType, String command, Location location, long createdAt) {
        execute("""
                INSERT INTO command_logs(executor, executor_type, command, world, x, y, z, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, statement -> {
            statement.setString(1, executor);
            statement.setString(2, executorType);
            statement.setString(3, command);
            if (location != null) {
                statement.setString(4, location.getWorld() == null ? "" : location.getWorld().getName());
                statement.setDouble(5, location.getX());
                statement.setDouble(6, location.getY());
                statement.setDouble(7, location.getZ());
            } else {
                statement.setString(4, "");
                statement.setDouble(5, 0);
                statement.setDouble(6, 0);
                statement.setDouble(7, 0);
            }
            statement.setLong(8, createdAt);
        });
    }

    public synchronized void incrementPlayerCounter(UUID uuid, String column, long amount) {
        if (!List.of("total_commands", "total_chat_messages", "total_deaths", "total_kills", "blocks_broken", "blocks_placed").contains(column)) {
            throw new IllegalArgumentException("Invalid counter column: " + column);
        }
        execute("UPDATE players SET " + column + "=" + column + " + ? WHERE uuid=?", statement -> {
            statement.setLong(1, amount);
            statement.setString(2, uuid.toString());
        });
    }

    public synchronized void addDistance(UUID uuid, double distance) {
        execute("UPDATE players SET distance_walked=distance_walked + ? WHERE uuid=?", statement -> {
            statement.setDouble(1, Math.max(0, distance));
            statement.setString(2, uuid.toString());
        });
    }

    public synchronized Optional<PlayerStats> findByUuid(UUID uuid) {
        try (PreparedStatement statement = database.connection().prepareStatement("SELECT * FROM players WHERE uuid=?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Erro ao buscar jogador: " + exception.getMessage());
        }
        return Optional.empty();
    }

    public synchronized Optional<PlayerStats> findByName(String name) {
        try (PreparedStatement statement = database.connection().prepareStatement("SELECT * FROM players WHERE lower(name)=lower(?) LIMIT 1")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Erro ao buscar jogador: " + exception.getMessage());
        }
        return Optional.empty();
    }

    public synchronized List<PlayerStats> top(String column, int limit) {
        if (!List.of("total_playtime_seconds", "total_commands", "total_deaths", "total_chat_messages").contains(column)) {
            throw new IllegalArgumentException("Invalid top column: " + column);
        }
        List<PlayerStats> stats = new ArrayList<>();
        try (PreparedStatement statement = database.connection().prepareStatement("SELECT * FROM players ORDER BY " + column + " DESC LIMIT ?")) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    stats.add(map(resultSet));
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Erro ao gerar ranking: " + exception.getMessage());
        }
        return stats;
    }

    private PlayerStats map(ResultSet resultSet) throws SQLException {
        return new PlayerStats(
                UUID.fromString(resultSet.getString("uuid")),
                resultSet.getString("name"),
                resultSet.getLong("first_join"),
                resultSet.getLong("last_join"),
                resultSet.getLong("last_quit"),
                resultSet.getLong("total_playtime_seconds"),
                resultSet.getLong("total_sessions"),
                resultSet.getLong("total_commands"),
                resultSet.getLong("total_chat_messages"),
                resultSet.getLong("total_deaths"),
                resultSet.getLong("total_kills"),
                resultSet.getLong("blocks_broken"),
                resultSet.getLong("blocks_placed"),
                resultSet.getDouble("distance_walked")
        );
    }

    private void execute(String sql, SqlBinder binder) {
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Erro SQLite: " + exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
