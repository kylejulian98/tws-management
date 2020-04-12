package tws.management.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import tws.management.data.callbacks.AfkKickExemptListQueryCallback;
import tws.management.data.callbacks.BooleanQueryCallback;
import tws.management.data.entities.AfkKickExemptList;

public class AfkDatabaseManager extends DatabaseManager {

	public AfkDatabaseManager(JavaPlugin plugin, DatabaseConnectionManager databaseConnectionManager) {
		super(plugin, databaseConnectionManager);
	}

	@Override
	public void setupDefaultSchema(final BooleanQueryCallback callback) {
		final String sqlCommand = "CREATE TABLE IF NOT EXISTS afk_kick_exempt (id SERIAL PRIMARY KEY NOT NULL, player_id UUID NOT NULL)";

		Runnable task = () -> {
			boolean result = false;

			try (Connection connection = this.getConnection();
					PreparedStatement statement = connection.prepareStatement(sqlCommand)) {
				result = statement.execute();
			} catch (SQLException e) {
				this.getPlugin().getLogger().log(Level.WARNING,
						"Unable to setup Default Schema for AFK Database tables.");
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
	public void addPlayerToKickExempt(final UUID playerId, final BooleanQueryCallback callback) {
		final String sqlCommand = "INSERT INTO afk_kick_exempt (player_id) VALUES (?)";

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
	public void removePlayerFromKickExempt(final UUID playerId, final BooleanQueryCallback callback) {
		final String sqlCommand = "DELETE FROM afk_kick_exempt WHERE player_id = ?";

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
	public void isPlayerKickExempt(final UUID playerId, final BooleanQueryCallback callback) {
		final String sqlCommand = "SELECT id FROM afk_kick_exempt WHERE player_id=?";

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
	public void getAfkKickExemptPlayers(final int pageIndex, final int pageSize, final AfkKickExemptListQueryCallback callback) {
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
					String rowPlayerId = set.getString("player_id");
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

	private void queueCallbackTaskSync(final BooleanQueryCallback callback, boolean result) {
		final boolean callbackResult = result;
		Runnable callbackTask = () -> {
			callback.onQueryComplete(callbackResult);
		};

		this.executeQuerySync(callbackTask);
	}
	
	private void queueCallbackTaskSync(final AfkKickExemptListQueryCallback callback, AfkKickExemptList result) {
		final AfkKickExemptList callbackResult = result;
		Runnable callbackTask = () -> {
			callback.onQueryComplete(callbackResult);
		};

		this.executeQuerySync(callbackTask);
	}
}
