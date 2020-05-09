package dev.kylejulian.tws.configuration;

public class DatabaseConfigModel {

	private String database, directory;
	private int maxConcurrentConnections;
	
	public void setDatabase(String database) {
		this.database = database;
	}

	public void setDirectory(String directory) { this.directory = directory; }

	public void setMaxConcurrentConnections(int maxConcurrentConnections) {
		this.maxConcurrentConnections = maxConcurrentConnections;
	}
	
	public String getDatabase() {
		return this.database;
	}

	public String getDirectory() { return this.directory; }

	public int getMaxConcurrentConnections() {
		return this.maxConcurrentConnections;
	}
}
