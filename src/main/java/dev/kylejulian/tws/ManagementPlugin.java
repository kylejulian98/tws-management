package dev.kylejulian.tws;

import dev.kylejulian.tws.afk.AfkEventListener;
import dev.kylejulian.tws.commands.AfkCommand;
import dev.kylejulian.tws.commands.HudCommand;
import dev.kylejulian.tws.commands.tabcompleters.AfkTabCompleter;
import dev.kylejulian.tws.configuration.*;
import dev.kylejulian.tws.data.DatabaseConnectionManager;
import dev.kylejulian.tws.data.MojangApi;
import dev.kylejulian.tws.data.interfaces.IAfkDatabaseManager;
import dev.kylejulian.tws.data.interfaces.IDatabaseManager;
import dev.kylejulian.tws.data.interfaces.IHudDatabaseManager;
import dev.kylejulian.tws.data.sqlite.AfkDatabaseManager;
import dev.kylejulian.tws.data.sqlite.HudDatabaseManager;
import dev.kylejulian.tws.player.PlayerListener;
import dev.kylejulian.tws.player.hud.HudListener;
import dev.kylejulian.tws.world.DaytimeListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ManagementPlugin extends JavaPlugin {

    private final ConfigurationManager configManager;
    private final DatabaseConnectionManager databaseConnectionManager; 
    
    private final IAfkDatabaseManager afkDatabaseManager;
    private final IHudDatabaseManager hudDatabaseManager;

    public ManagementPlugin() {
    	this.configManager = new ConfigurationManager(this, "config.json");
    	this.configManager.reload();
    	ConfigModel config = this.configManager.getConfig();
    	DatabaseConfigModel databaseConfig = config.getDatabaseConfig(); 
    	
    	this.databaseConnectionManager = new DatabaseConnectionManager(databaseConfig);
    	this.afkDatabaseManager = new AfkDatabaseManager(this, this.databaseConnectionManager);
    	this.hudDatabaseManager = new HudDatabaseManager(this, this.databaseConnectionManager);
    }
	
	@Override
	public void onEnable() {
		ConfigModel config = this.configManager.getConfig();
		AfkConfigModel afkConfig = config.getAfkConfig();
		NightResetConfigModel nightResetConfig = config.getNightResetConfig();
		HudConfigModel hudConfig = config.getHudConfig();

		runDefaultSchemaSetup(new IDatabaseManager[] { this.afkDatabaseManager, this.hudDatabaseManager } );
		
		this.getServer().getPluginManager().registerEvents(new PlayerListener(this, this.afkDatabaseManager,
				this.hudDatabaseManager, this.configManager), this);
		this.getServer().getPluginManager().registerEvents(new AfkEventListener(this, afkConfig), this);
		this.getServer().getPluginManager().registerEvents(new DaytimeListener(this, nightResetConfig), this);
		this.getServer().getPluginManager().registerEvents(new HudListener(this, hudConfig), this);
		
		Objects.requireNonNull(this.getCommand("afk")).setExecutor(new AfkCommand(this, this.afkDatabaseManager, new MojangApi()));
		Objects.requireNonNull(this.getCommand("afk")).setTabCompleter(new AfkTabCompleter(this));
		Objects.requireNonNull(this.getCommand("hud")).setExecutor(new HudCommand(this, this.hudDatabaseManager));
	}
	
	@Override
	public void onDisable() {
		this.databaseConnectionManager.closeConnections();
	}

	private void runDefaultSchemaSetup(@NotNull IDatabaseManager[] databaseManagers) {
    	for (IDatabaseManager databaseManager : databaseManagers) {
    		databaseManager.setupDefaultSchema(null);
		}
	}
}
