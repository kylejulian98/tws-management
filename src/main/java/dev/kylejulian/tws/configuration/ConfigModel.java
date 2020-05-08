package dev.kylejulian.tws.configuration;

public class ConfigModel {
	
	private AfkConfigModel afkConfig;
	private NightResetConfig nightResetConfig;
	private DatabaseConfigModel databaseConfig;

	public AfkConfigModel getAfkConfig() {
		return this.afkConfig;
	}

	public NightResetConfig getNightResetConfig() { return this.nightResetConfig; }
	
	public DatabaseConfigModel getDatabaseConfig() {
		return this.databaseConfig;
	}
	
	public void setAfkConfig(AfkConfigModel afkConfig) {
		this.afkConfig = afkConfig;
	}

	public void setNightResetConfig(NightResetConfig nightResetConfig) { this.nightResetConfig = nightResetConfig; }
	
	public void setDatabaseConfig(DatabaseConfigModel databaseConfig) {
		this.databaseConfig = databaseConfig;
	}
}
