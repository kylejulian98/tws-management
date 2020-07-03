package dev.kylejulian.tws.data.sqlite;

import dev.kylejulian.tws.data.DatabaseConnectionManager;
import dev.kylejulian.tws.data.DatabaseManager;
import dev.kylejulian.tws.data.entities.EntityExemptList;
import dev.kylejulian.tws.data.interfaces.IExemptDatabaseManager;
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

public class AfkDatabaseManager extends DatabaseManager implements IExemptDatabaseManager {

	public AfkDatabaseManager(JavaPlugin plugin, DatabaseConnectionManager databaseConnectionManager) {
		super(plugin, databaseConnectionManager);
	}

	@Override
	public @NotNull CompletableFuture<Void> setupDefaultSchema() {
		return this
				.execute("CREATE TABLE IF NOT EXISTS afk_kick_exempt (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, player_uuid UUID NOT NULL)", null)
				.thenCompose(aVoid -> this.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_afk_kick_exempt_id ON afk_kick_exempt (id)", null))
				.thenCompose(aVoid -> this.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_afk_kick_exempt_player_uuid ON afk_kick_exempt (player_uuid)", null));
	}

	/**
	 * Adds a specified Player to the exempt list
	 *
	 * @param playerId Player to remove from the Exempt list
	 */
	@Override
	public @NotNull CompletableFuture<Void> add(final @NotNull UUID playerId) {
		return this.execute("INSERT INTO afk_kick_exempt (player_uuid) VALUES (?)", new Object[]{ playerId });
	}

	/**
	 * Removes a specified Player to the exempt list
	 *
	 * @param playerId Player to remove from the Exempt list
	 */
	@Override
	public @NotNull CompletableFuture<Void> remove(final @NotNull UUID playerId) {
		return this.execute("DELETE FROM afk_kick_exempt WHERE player_uuid = ?", new Object[]{ playerId });
	}

	/**
	 * Returns whether or not a Player is exempt from being kicked due to inactivity
	 * 
	 * @param playerId Player to lookup
	 */
	@Override
	public @NotNull CompletableFuture<Boolean> isExempt(final @NotNull UUID playerId) {
		return this.exists("SELECT id FROM afk_kick_exempt WHERE player_uuid=?", new Object[]{ playerId });
	}
	
	/**
	 * Gets a list of all AFK Kick Exempt Players
	 * @param pageIndex What page to start at. Should always be greater than 1.
	 * @param pageSize Number of results to be returned
	 */
	@Override
	public @NotNull CompletableFuture<EntityExemptList> getPlayers(final int pageIndex, final int pageSize) {
		final String sqlCommand = "SELECT * FROM afk_kick_exempt LIMIT ? OFFSET ?";
		final String sqlCountCommand = "SELECT COUNT(*) FROM afk_kick_exempt";
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
				this.getPlugin().getLogger().log(Level.WARNING, "Unable to execute query for AFK kick check.");
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
}
