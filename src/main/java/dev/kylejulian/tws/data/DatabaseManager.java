package dev.kylejulian.tws.data;

import dev.kylejulian.tws.data.interfaces.IDatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public abstract class DatabaseManager implements IDatabaseManager {

	private final DatabaseConnectionManager databaseConnectionManager;
	private final JavaPlugin plugin;

	public DatabaseManager(JavaPlugin plugin, DatabaseConnectionManager databaseConnectionManager) {
		this.plugin = plugin;
		this.databaseConnectionManager = databaseConnectionManager;
	}

	/**
	 * Setups the Default Database schema for the Database Manager
	 *
     * @return CompletableFuture<Void> which will setup the Database schema
     */
	public abstract @NotNull CompletableFuture<Void> setupDefaultSchema();

	protected JavaPlugin getPlugin() {
		return this.plugin;
	}

	/**
	 * Gets a Connection from the Connection pool
	 * 
	 * @return Returns the created or existing Connection
	 * @throws SQLException If a database access issue occurs
	 */
	protected Connection getConnection() throws SQLException {
		return this.databaseConnectionManager.getConnectionPool().getConnection();
	}

	/**
	 * Executes a sql command with specified params. The params must be in the order they appear in the sql string.
	 * @param sql Sql command to execute
	 * @param params Array of collections intended to be parameterised in the Sql query
	 * @return A CompletableFuture<Void> with the Sql command executed
	 */
	protected CompletableFuture<Void> execute(final String sql, final Object[] params) {
		return CompletableFuture.runAsync(() -> {
			try (Connection connection = this.getConnection();
				 PreparedStatement statement = connection.prepareStatement(sql)) {

				if (params != null) {
					buildQueryParameters(statement, params);
				}

				statement.execute();
			} catch (SQLException e) {
				this.getPlugin().getLogger().log(Level.WARNING, "Unable to execute query.");
				this.getPlugin().getLogger().log(Level.WARNING, e.getMessage());
			}
		});
	}

	/**
	 * Executes a sql command with specified params. The params must be in the order they appear in the sql string.
	 * Returns whether or not at least one row existed with for the given query.
	 * @param sql Sql command to execute
	 * @param params Array of collections intended to be parameterised in the Sql query
	 * @return A CompletableFuture<Boolean> with the Sql command executed
	 */
	protected CompletableFuture<Boolean> exists(final String sql, final Object[] params) {
		return CompletableFuture.supplyAsync(() -> {
			boolean result = false;
			ResultSet set = null;

			try (Connection connection = this.getConnection();
				 PreparedStatement statement = connection.prepareStatement(sql)) {

				if (params != null) {
					buildQueryParameters(statement, params);
				}

				set = statement.executeQuery();
				int count = 0; // Should only ever be 1

				while (set.next()) {
					count++;
				}

				result = count >= 1;
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
			}

			return result;
		});
	}

	/**
	 * Adds the provided Object params to the Sql statement
	 * @param statement Sql command to add the parameters
	 * @param params The parameters that are used in the query
	 * @throws SQLException If the number of query parameters in the sql statement do not match the number of params
	 * provided in the specified params
	 */
	private void buildQueryParameters(final PreparedStatement statement, final Object[] params) throws SQLException {
		for (int i = 1; i <= params.length; i++) {
			statement.setObject(i, params[i]);
		}
	}
}
