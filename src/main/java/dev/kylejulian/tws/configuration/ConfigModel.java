package dev.kylejulian.tws.configuration;

public class ConfigModel {
	
	private AfkConfigModel afkConfig;
	private NightResetConfigModel nightResetConfig;
	private DatabaseConfigModel databaseConfig;
	private HudConfigModel hudConfig;

	public AfkConfigModel getAfkConfig() {
		return this.afkConfig;
	}

	public NightResetConfigModel getNightResetConfig() { return this.nightResetConfig; }
	
	public DatabaseConfigModel getDatabaseConfig() {
		return this.databaseConfig;
	}

	public HudConfigModel getHudConfig() { return this.hudConfig; }
	
	public void setAfkConfig(AfkConfigModel afkConfig) {
		this.afkConfig = afkConfig;
	}

	public void setNightResetConfig(NightResetConfigModel nightResetConfig) { this.nightResetConfig = nightResetConfig; }
	
	public void setDatabaseConfig(DatabaseConfigModel databaseConfig) {
		this.databaseConfig = databaseConfig;
	}

	public void setHudConfig(HudConfigModel hudConfig) { this.hudConfig = hudConfig; }
}
