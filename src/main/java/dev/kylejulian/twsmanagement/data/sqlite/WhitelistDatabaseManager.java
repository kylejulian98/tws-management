package dev.kylejulian.twsmanagement.data.sqlite;

import dev.kylejulian.twsmanagement.data.DatabaseConnectionManager;
import dev.kylejulian.twsmanagement.data.DatabaseManager;
import dev.kylejulian.twsmanagement.data.entities.EntityExemptList;
import dev.kylejulian.twsmanagement.data.interfaces.IExemptDatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WhitelistDatabaseManager extends DatabaseManager implements IExemptDatabaseManager {

    public WhitelistDatabaseManager(JavaPlugin plugin, DatabaseConnectionManager databaseConnectionManager) {
        super(plugin, databaseConnectionManager);
    }

    @Override
    public @NotNull CompletableFuture<Void> setupDefaultSchema() {
        return this
                .execute("CREATE TABLE IF NOT EXISTS whitelist_exempt (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, player_uuid UUID NOT NULL)", null)
                .thenCompose(aVoid -> this.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_whitelist_exempt_id ON whitelist_exempt (id)", null))
                .thenCompose(aVoid -> this.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_whitelist_exempt_player_uuid ON whitelist_exempt (player_uuid)", null));
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isExempt(@NotNull UUID playerId) {
        return this.exists("SELECT id FROM whitelist_exempt WHERE player_uuid=?", new Object[]{ playerId });
    }

    @Override
    public @NotNull CompletableFuture<EntityExemptList> getPlayers(int pageIndex, int pageSize) {
        final String sqlCommand = "SELECT * FROM whitelist_exempt LIMIT ? OFFSET ?";
        final String sqlCountCommand = "SELECT COUNT(*) FROM whitelist_exempt";
        int offset = pageSize * (pageIndex - 1);
        this.getPlugin().getLogger().log(Level.FINE, "Offset for Pagination is [" + offset + "].");

        return CompletableFuture.supplyAsync(() -> {
            EntityExemptList result = new EntityExemptList();
            ArrayList<UUID> playerIds = new ArrayList<>();
            ResultSet set = null;
            ResultSet countSet = null;
            PreparedStatement countStatement = null;

            try (Connection connection = this.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sqlCommand)) {
                statement.setInt(1, pageSize);
                statement.setInt(2, offset);

                set = statement.executeQuery();

                while (set.next()) {
                    String rowPlayerId = set.getString("player_uuid");
                    UUID playerId = UUID.fromString(rowPlayerId);
                    playerIds.add(playerId);
                }

                countStatement = connection.prepareStatement(sqlCountCommand);
                countSet = countStatement.executeQuery();

                while (countSet.next()) {
                    int count = countSet.getInt(1);
                    int maxPages =  (int) Math.ceil(count / (double) pageSize);
                    result.setMaxPageCount(maxPages);
                }

            } catch (SQLException e) {
                this.getPlugin().getLogger().log(Level.WARNING, "Unable to execute query for Whitelist exempt check.");
                this.getPlugin().getLogger().log(Level.WARNING, e.getMessage());
            } finally {
                if (set != null) {
                    try {
                        set.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (countSet != null) {
                    try {
                        countSet.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                if (countStatement != null) {
                    try {
                        countStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            result.setPlayerIds(playerIds);

            return result;
        });
    }

    @Override
    public @NotNull CompletableFuture<Void> add(@NotNull UUID playerId) {
        return this.execute("INSERT INTO whitelist_exempt (player_uuid) VALUES (?)", new Object[]{ playerId });
    }

    @Override
    public @NotNull CompletableFuture<Void> remove(@NotNull UUID playerId) {
        return this.execute("DELETE FROM whitelist_exempt WHERE player_uuid = ?", new Object[]{ playerId });
    }

    @Override
    public @NotNull CompletableFuture<Void> clear() {
        return this.execute("DELETE FROM whitelist_exempt", new Object[0]);
    }
}
