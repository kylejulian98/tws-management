package dev.kylejulian.tws.configuration;

public class DatabaseConfigModel {

	private String database, directory, userName, password;
	private int port;
	private int maxConcurrentConnections;
	
	public void setDatabase(String database) {
		this.database = database;
	}

	public void setDirectory(String directory) { this.directory = directory; }
	
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public void setMaxConcurrentConnections(int maxConcurrentConnections) {
		this.maxConcurrentConnections = maxConcurrentConnections;
	}
	
	public String getDatabase() {
		return this.database;
	}

	public String getDirectory() { return this.directory; }

	public String getUserName() {
		return this.userName;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public int getMaxConcurrentConnections() {
		return this.maxConcurrentConnections;
	}
}
