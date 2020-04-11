package tws.management.configuration;

public class DatabaseConfigModel {

	private String host, database, userName, password;
	private int port;
	private int maxConcurrentConnections;
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public void setDatabase(String database) {
		this.database = database;
	}
	
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
	
	public String getHost() {
		return this.host;
	}
	
	public String getDatabase() {
		return this.database;
	}
	
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
