package tws.management;

import org.bukkit.plugin.java.JavaPlugin;

import tws.management.afk.AfkEventListener;
import tws.management.commands.AfkCommand;
import tws.management.configuration.AfkConfigModel;
import tws.management.configuration.ConfigModel;
import tws.management.configuration.ConfigurationManager;
import tws.management.configuration.DatabaseConfigModel;
import tws.management.data.AfkDatabaseManager;
import tws.management.data.DatabaseConnectionManager;
import tws.management.data.MojangApi;
import tws.management.player.PlayerListener;

public class ManagementPlugin extends JavaPlugin {

    private final ConfigurationManager configManager;
    private final DatabaseConnectionManager databaseConnectionManager; 
    
    private final AfkDatabaseManager afkDatabase;
    
    public ManagementPlugin() {
    	this.configManager = new ConfigurationManager(this, "config.json");
    	this.configManager.reload();
    	ConfigModel config = this.configManager.getConfig();
    	DatabaseConfigModel databaseConfig = config.getDatabaseConfig(); 
    	
    	this.databaseConnectionManager = new DatabaseConnectionManager(databaseConfig);
    	this.afkDatabase = new AfkDatabaseManager(this, this.databaseConnectionManager);
    }
	
	@Override
	public void onEnable() {
		ConfigModel config = this.configManager.getConfig();
		AfkConfigModel afkConfig = config.getAfkConfig();
		
		this.afkDatabase.setupDefaultSchema(null);
		
		this.getServer().getPluginManager().registerEvents(new PlayerListener(this, this.afkDatabase, this.configManager), this);
		this.getServer().getPluginManager().registerEvents(new AfkEventListener(this, afkConfig), this);
		
		this.getCommand("afk").setExecutor(new AfkCommand(this, this.afkDatabase, new MojangApi()));
	}
	
	@Override
	public void onDisable() {
		
	}
}
