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
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ManagementPlugin extends JavaPlugin {

    private final ConfigurationManager configManager;
    private DatabaseConnectionManager databaseConnectionManager;

	public ManagementPlugin() {
    	this.configManager = new ConfigurationManager(this, "config.json");
    }
	
	@Override
	public void onEnable() {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		this.configManager.reload();
		this.getLogger().log(Level.INFO, "Plugin Configuration has been loaded by {0}ms", stopWatch.getTime());

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

		this.databaseConnectionManager = new DatabaseConnectionManager(databaseConfig, this.getDataFolder().getAbsolutePath());
		IAfkDatabaseManager afkDatabaseManager = new AfkDatabaseManager(this, this.databaseConnectionManager);
		IHudDatabaseManager hudDatabaseManager = new HudDatabaseManager(this, this.databaseConnectionManager);

		this.getLogger().log(Level.INFO, "Internal dependencies have been created by {0}ms", stopWatch.getTime());

		runDefaultSchemaSetup(new IDatabaseManager[] {afkDatabaseManager, hudDatabaseManager} );

		this.getLogger().log(Level.INFO, "Database schemas have been validated by {0}ms", stopWatch.getTime());
		
		this.getServer().getPluginManager().registerEvents(new PlayerListener(this, afkDatabaseManager,
				hudDatabaseManager, this.configManager), this);
		this.getServer().getPluginManager().registerEvents(new AfkEventListener(this, afkConfig), this);
		this.getServer().getPluginManager().registerEvents(new DaytimeListener(this, nightResetConfig), this);
		this.getServer().getPluginManager().registerEvents(new HudListener(this, hudConfig), this);

		this.getLogger().log(Level.INFO, "Plugin Events have been registered by {0}ms", stopWatch.getTime());

		this.getServer().getScheduler().runTaskTimerAsynchronously(this, new WhitelistRunnable(this, whitelistConfig), 0, 36000);
		
		Objects.requireNonNull(this.getCommand("afk")).setExecutor(new AfkCommand(this, afkDatabaseManager, new MojangApi(this.getLogger())));
		Objects.requireNonNull(this.getCommand("afk")).setTabCompleter(new AfkTabCompleter(this));
		Objects.requireNonNull(this.getCommand("hud")).setExecutor(new HudCommand(this, hudDatabaseManager));

		stopWatch.stop();
		this.getLogger().log(Level.INFO, "Plugin started in {0}ms", stopWatch.getTime());
	}

	@Override
	public void onDisable() {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		this.databaseConnectionManager.closeConnections();
		this.getLogger().log(Level.INFO, "All database connections have been closed in {0}ms", stopWatch.getTime());

		stopWatch.stop();
		this.getLogger().log(Level.INFO, "Plugin stopped in {0}ms", stopWatch.getTime());
	}

	private void runDefaultSchemaSetup(@NotNull IDatabaseManager[] databaseManagers) {
		CompletableFuture<?>[] completableFutures = new CompletableFuture<?>[databaseManagers.length];
    	for (int i = 0; i < databaseManagers.length; i++) {
			completableFutures[i] = databaseManagers[i].setupDefaultSchema();
		}
    	CompletableFuture.allOf(completableFutures);
	}
}
