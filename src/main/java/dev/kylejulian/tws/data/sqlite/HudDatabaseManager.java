package dev.kylejulian.tws.data.sqlite;

import dev.kylejulian.tws.data.DatabaseConnectionManager;
import dev.kylejulian.tws.data.DatabaseManager;
import dev.kylejulian.tws.data.callbacks.BooleanQueryCallback;
import dev.kylejulian.tws.data.interfaces.IHudDatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class HudDatabaseManager extends DatabaseManager implements IHudDatabaseManager {

    public HudDatabaseManager(JavaPlugin plugin, DatabaseConnectionManager databaseConnectionManager) {
        super(plugin, databaseConnectionManager);
    }

    @Override
    public void setupDefaultSchema(BooleanQueryCallback callback) {
        final String sqlCommand = "CREATE TABLE IF NOT EXISTS hud (id INTEGER PRIMARY KEY NOT NULL, player_uuid UUID NOT NULL)";
        final String sqlIndexCommand = "CREATE UNIQUE INDEX IF NOT EXISTS idx_hud_id ON hud (id)";
        final String sqlPlayerIdIndexCommand = "CREATE UNIQUE INDEX IF NOT EXISTS idx_hud_player_uuid ON hud (player_uuid)";

        Runnable task = () -> {
            boolean result = false;

            try (Connection connection = this.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sqlCommand)) {
                result = statement.execute();
            } catch (SQLException e) {
                this.getPlugin().getLogger().log(Level.WARNING,
                        "Unable to setup Default Schema for Hud Database table.");
                this.getPlugin().getLogger().log(Level.WARNING, e.getMessage());
            }

            try (Connection connection = this.getConnection();
                PreparedStatement indexStatement = connection.prepareStatement(sqlIndexCommand);
                PreparedStatement indexPlayerIdStatement = connection.prepareStatement(sqlPlayerIdIndexCommand)) {
                boolean indexResult = indexStatement.execute();
                boolean playerIdIndexResult = indexPlayerIdStatement.execute();
                result = result && indexResult && playerIdIndexResult;
            } catch (SQLException e) {
                this.getPlugin().getLogger().log(Level.WARNING,
                        "Unable to setup Default Indexes for Hud Database table.");
                this.getPlugin().getLogger().log(Level.WARNING, e.getMessage());
            }

            if (callback != null) {
                queueCallbackTaskSync(callback, result);
            }
        };

        this.executeQueryAsync(task);
    }

    @Override
    public void isEnabled(UUID playerId, BooleanQueryCallback callback) {
        final String sqlCommand = "SELECT id FROM hud WHERE player_uuid = ?";

        Runnable task = () -> {
            boolean result = false;
            ResultSet set = null;

            try (Connection connection = this.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sqlCommand)) {
                statement.setObject(1, playerId);

                set = statement.executeQuery();
                int count = 0; // Should only ever be 1

                while (set.next()) {
                    count++;
                }

                result = count == 1;
            } catch (SQLException e) {
                this.getPlugin().getLogger().log(Level.WARNING, "Unable to execute query for Hud.");
                this.getPlugin().getLogger().log(Level.WARNING, e.getMessage());
            } finally {
                if (set != null) {
                    try {
                        set.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (callback != null) {
                queueCallbackTaskSync(callback, result);
            }
        };

        this.executeQueryAsync(task);
    }

    @Override
    public void removePlayer(UUID playerId, BooleanQueryCallback callback) {
        final String sqlCommand = "DELETE FROM hud WHERE player_uuid = ?";

        Runnable task = () -> {
            boolean result = false;

            try (Connection connection = this.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sqlCommand)) {
                statement.setObject(1, playerId);

                result = statement.execute();
            } catch (SQLException e) {
                this.getPlugin().getLogger().log(Level.WARNING, "Unable to execute query for Hud.");
                this.getPlugin().getLogger().log(Level.WARNING, e.getMessage());
            }

            if (callback != null) {
                queueCallbackTaskSync(callback, result);
            }
        };

        this.executeQueryAsync(task);
    }

    @Override
    public void addPlayer(UUID playerId, BooleanQueryCallback callback) {
        final String sqlCommand = "INSERT INTO hud (player_uuid) VALUES (?)";

        Runnable task = () -> {
            boolean result = false;

            try (Connection connection = this.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sqlCommand)) {
                statement.setObject(1, playerId);

                result = statement.execute();
            } catch (SQLException e) {
                this.getPlugin().getLogger().log(Level.WARNING, "Unable to execute query for Hud.");
                this.getPlugin().getLogger().log(Level.WARNING, e.getMessage());
            }

            if (callback != null) {
                queueCallbackTaskSync(callback, result);
            }
        };

        this.executeQueryAsync(task);
    }
}
