package dev.kylejulian.tws.data;

import org.apache.commons.dbcp2.BasicDataSource;

import dev.kylejulian.tws.configuration.DatabaseConfigModel;

public class DatabaseConnectionManager {

	private final BasicDataSource connectionPool;

	public DatabaseConnectionManager(DatabaseConfigModel databaseConfig) {
		this.connectionPool = new BasicDataSource();
		this.connectionPool.setDriverClassName("org.sqlite.JDBC");
		this.connectionPool.setUrl("jdbc:sqlite:" + databaseConfig.getDirectory() + "\\" + databaseConfig.getDatabase());
		this.connectionPool.setUsername(databaseConfig.getUserName());
		this.connectionPool.setPassword(databaseConfig.getPassword());
		this.connectionPool.setInitialSize(1);
		this.connectionPool.setMaxTotal(databaseConfig.getMaxConcurrentConnections());
	}
	
	public BasicDataSource getConnectionPool() {
		return this.connectionPool;
	}
}
