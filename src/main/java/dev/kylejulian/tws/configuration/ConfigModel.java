package dev.kylejulian.tws.configuration;

public class ConfigModel {
	
	private AfkConfigModel afkConfig;
	private DatabaseConfigModel databaseConfig;
	
	public AfkConfigModel getAfkConfig() {
		return this.afkConfig;
	}
	
	public DatabaseConfigModel getDatabaseConfig() {
		return this.databaseConfig;
	}
	
	public void setAfkConfig(AfkConfigModel afkConfig) {
		this.afkConfig = afkConfig;
	}
	
	public void setDatabaseConfig(DatabaseConfigModel databaseConfig) {
		this.databaseConfig = databaseConfig;
	}
}
