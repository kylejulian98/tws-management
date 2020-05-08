package dev.kylejulian.tws;

import org.bukkit.plugin.java.JavaPlugin;

import dev.kylejulian.tws.afk.AfkEventListener;
import dev.kylejulian.tws.commands.AfkCommand;
import dev.kylejulian.tws.commands.tabcompleters.AfkTabCompleter;
import dev.kylejulian.tws.configuration.AfkConfigModel;
import dev.kylejulian.tws.configuration.ConfigModel;
import dev.kylejulian.tws.configuration.ConfigurationManager;
import dev.kylejulian.tws.configuration.DatabaseConfigModel;
import dev.kylejulian.tws.data.sqlite.AfkDatabaseManager;
import dev.kylejulian.tws.data.DatabaseConnectionManager;
import dev.kylejulian.tws.data.MojangApi;
import dev.kylejulian.tws.player.PlayerListener;

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
		this.getCommand("afk").setTabCompleter(new AfkTabCompleter(this));
	}
	
	@Override
	public void onDisable() {
		
	}
}
