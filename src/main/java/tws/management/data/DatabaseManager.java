package tws.management.data;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public abstract class DatabaseManager {

	private final DatabaseConnectionManager databaseConnectionManager;
	private final JavaPlugin plugin;

	public DatabaseManager(JavaPlugin plugin, DatabaseConnectionManager databaseConnectionManager) {
		this.plugin = plugin;
		this.databaseConnectionManager = databaseConnectionManager;
	}

	/**
	 * Setups the Default Database schema for the Database Manager
	 * 
	 * @param callback The result of the Query as a {@link QueryCallback}
	 */
	public abstract void setupDefaultSchema(final QueryCallback callback);

	protected JavaPlugin getPlugin() {
		return this.plugin;
	}

	/**
	 * Queues up a Runnable task on the Server Scheduler asynchronously
	 * 
	 * @param runnable
	 * @return The created BukkitTask
	 */
	protected BukkitTask executeQueryAsync(Runnable runnable) {
		return this.getPlugin().getServer().getScheduler().runTaskAsynchronously(this.plugin, runnable);
	}

	/**
	 * Queues up a Runnable task on the Server scheduler synchronously
	 * 
	 * @param runnable
	 * @return
	 */
	protected BukkitTask executeQuerySync(Runnable runnable) {
		return this.getPlugin().getServer().getScheduler().runTask(this.plugin, runnable);
	}

	/**
	 * Gets a Connection from the Connection pool
	 * 
	 * @return Returns the created or existing Connection
	 * @throws SQLException
	 */
	protected Connection getConnection() throws SQLException {
		return this.databaseConnectionManager.getConnectionPool().getConnection();
	}
}
