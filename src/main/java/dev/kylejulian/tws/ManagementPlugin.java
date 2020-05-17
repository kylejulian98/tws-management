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
import dev.kylejulian.tws.server.whitelist.WhitelistRunnable;
import dev.kylejulian.tws.world.DaytimeListener;
import org.apache.commons.lang.NullArgumentException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.logging.Level;

public class ManagementPlugin extends JavaPlugin {

    private final ConfigurationManager configManager;
    private DatabaseConnectionManager databaseConnectionManager;

	public ManagementPlugin() {
    	this.configManager = new ConfigurationManager(this, "config.json");
    }
	
	@Override
	public void onEnable() {
		this.configManager.reload();

		ConfigModel config = this.configManager.getConfig();
		DatabaseConfigModel databaseConfig = config.getDatabaseConfig();
		AfkConfigModel afkConfig = config.getAfkConfig();
		NightResetConfigModel nightResetConfig = config.getNightResetConfig();
		HudConfigModel hudConfig = config.getHudConfig();
		WhitelistConfigModel whitelistConfig = config.getWhitelistConfig();

		if (databaseConfig == null || afkConfig == null || nightResetConfig == null ||
				hudConfig == null || whitelistConfig == null) {
			this.getLogger().log(Level.SEVERE, "Failed start up. Unable to get configuration for plugin.");
			return;
		}

		this.databaseConnectionManager = new DatabaseConnectionManager(databaseConfig);
		IAfkDatabaseManager afkDatabaseManager = new AfkDatabaseManager(this, this.databaseConnectionManager);
		IHudDatabaseManager hudDatabaseManager = new HudDatabaseManager(this, this.databaseConnectionManager);

		runDefaultSchemaSetup(new IDatabaseManager[] {afkDatabaseManager, hudDatabaseManager} );
		
		this.getServer().getPluginManager().registerEvents(new PlayerListener(this, afkDatabaseManager,
				hudDatabaseManager, this.configManager), this);
		this.getServer().getPluginManager().registerEvents(new AfkEventListener(this, afkConfig), this);
		this.getServer().getPluginManager().registerEvents(new DaytimeListener(this, nightResetConfig), this);
		this.getServer().getPluginManager().registerEvents(new HudListener(this, hudConfig), this);
		this.getServer().getScheduler().runTaskTimerAsynchronously(this, new WhitelistRunnable(this, whitelistConfig), 0, 60);
		
		Objects.requireNonNull(this.getCommand("afk")).setExecutor(new AfkCommand(this, afkDatabaseManager, new MojangApi()));
		Objects.requireNonNull(this.getCommand("afk")).setTabCompleter(new AfkTabCompleter(this));
		Objects.requireNonNull(this.getCommand("hud")).setExecutor(new HudCommand(this, hudDatabaseManager));
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
