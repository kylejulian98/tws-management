package dev.kylejulian.tws.data.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import dev.kylejulian.tws.data.DatabaseConnectionManager;
import dev.kylejulian.tws.data.DatabaseManager;
import dev.kylejulian.tws.data.interfaces.IAfkDatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import dev.kylejulian.tws.data.callbacks.AfkKickExemptListQueryCallback;
import dev.kylejulian.tws.data.callbacks.BooleanQueryCallback;
import dev.kylejulian.tws.data.entities.AfkKickExemptList;

public class AfkDatabaseManager extends DatabaseManager implements IAfkDatabaseManager {

	public AfkDatabaseManager(JavaPlugin plugin, DatabaseConnectionManager databaseConnectionManager) {
		super(plugin, databaseConnectionManager);
	}

	@Override
	public void setupDefaultSchema(final BooleanQueryCallback callback) {
		final String sqlCommand = "CREATE TABLE IF NOT EXISTS afk_kick_exempt (id INTEGER PRIMARY KEY NOT NULL, player_uuid UUID NOT NULL)";
		final String sqlIndexCommand = "CREATE UNIQUE INDEX IF NOT EXISTS idx_afk_kick_exempt_id ON afk_kick_exempt (id)";
		final String sqlPlayerIdIndexCommand = "CREATE UNIQUE INDEX IF NOT EXISTS idx_afk_kick_exempt_player_uuid ON afk_kick_exempt (player_uuid)";

		Runnable task = () -> {
			boolean result = false;

			try (Connection connection = this.getConnection();
					PreparedStatement statement = connection.prepareStatement(sqlCommand)) {
				result = statement.execute();
			} catch (SQLException e) {
				this.getPlugin().getLogger().log(Level.WARNING,
						"Unable to setup Default Schema for AFK Database table.");
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
						"Unable to setup Default Indexes for AFK Database table.");
				this.getPlugin().getLogger().log(Level.WARNING, e.getMessage());
			}

			if (callback != null) {
				queueCallbackTaskSync(callback, result);
			}
		};

		this.executeQueryAsync(task);
	}

	/**
	 * Adds a specified Player to the exempt list
	 * 
	 * @param callback The result of the Query as a {@link BooleanQueryCallback}
	 * @param playerId Player to remove from the Exempt list
	 */
	@Override
	public void addPlayer(final UUID playerId, final BooleanQueryCallback callback) {
		final String sqlCommand = "INSERT INTO afk_kick_exempt (player_uuid) VALUES (?)";

		Runnable task = () -> {
			boolean result = false;

			try (Connection connection = this.getConnection();
					PreparedStatement statement = connection.prepareStatement(sqlCommand)) {
				statement.setObject(1, playerId);

				result = statement.execute();
			} catch (SQLException e) {
				this.getPlugin().getLogger().log(Level.WARNING, "Unable to execute query for AFK kick check.");
				this.getPlugin().getLogger().log(Level.WARNING, e.getMessage());
			}

			if (callback != null) {
				queueCallbackTaskSync(callback, result);
			}
		};

		this.executeQueryAsync(task);
	}

	/**
	 * Removes a specified Player to the exempt list
	 * 
	 * @param callback The result of the Query as a {@link BooleanQueryCallback}
	 * @param playerId Player to remove from the Exempt list
	 */
	@Override
	public void removePlayer(final UUID playerId, final BooleanQueryCallback callback) {
		final String sqlCommand = "DELETE FROM afk_kick_exempt WHERE player_uuid = ?";

		Runnable task = () -> {
			boolean result = false;

			try (Connection connection = this.getConnection();
					PreparedStatement statement = connection.prepareStatement(sqlCommand)) {
				statement.setObject(1, playerId);

				result = statement.execute();
			} catch (SQLException e) {
				this.getPlugin().getLogger().log(Level.WARNING, "Unable to execute query for AFK kick check.");
				this.getPlugin().getLogger().log(Level.WARNING, e.getMessage());
			}

			if (callback != null) {
				queueCallbackTaskSync(callback, result);
			}
		};

		this.executeQueryAsync(task);
	}

	/**
	 * Returns whether or not a Player is exempt from being kicked due to inactivity
	 * 
	 * @param playerId Player to lookup
	 * @param callback The result of the Query as a {@link BooleanQueryCallback}
	 */
	@Override
	public void isKickExempt(final UUID playerId, final BooleanQueryCallback callback) {
		final String sqlCommand = "SELECT id FROM afk_kick_exempt WHERE player_uuid=?";

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
			}

			if (callback != null) {
				queueCallbackTaskSync(callback, result);
			}
		};

		this.executeQueryAsync(task);
	}
	
	/**
	 * Gets a list of all AFK Kick Exempt Players
	 * @param pageIndex What page to start at. Should always be greater than 1.
	 * @param pageSize Number of results to be returned
	 * @param callback The result of the Query as a {@link AfkKickExemptListQueryCallback}
	 */
	@Override
	public void getPlayers(final int pageIndex, final int pageSize, final AfkKickExemptListQueryCallback callback) {
		final String sqlCommand = "SELECT * FROM afk_kick_exempt LIMIT ? OFFSET ?";
		final String sqlCountCommand = "SELECT COUNT(*) FROM afk_kick_exempt";
		int offset = pageSize * (pageIndex - 1);
		this.getPlugin().getLogger().log(Level.FINE, "Offset for Pagination is [" + offset + "].");
		
		Runnable task = () -> {
			AfkKickExemptList result = new AfkKickExemptList();
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
					result.setPageCount(maxPages);
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

			if (callback != null) {
				queueCallbackTaskSync(callback, result);
			}
		};

		this.executeQueryAsync(task);
	}

	private void queueCallbackTaskSync(final AfkKickExemptListQueryCallback callback, AfkKickExemptList result) {
		final AfkKickExemptList callbackResult = result;
		Runnable callbackTask = () -> callback.onQueryComplete(callbackResult);

		this.executeQuerySync(callbackTask);
	}
}
