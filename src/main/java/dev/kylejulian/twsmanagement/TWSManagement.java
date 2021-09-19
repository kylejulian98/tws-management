package dev.kylejulian.twsmanagement;

import dev.kylejulian.twsmanagement.afk.AfkEventListener;
import dev.kylejulian.twsmanagement.commands.AfkCommand;
import dev.kylejulian.twsmanagement.commands.HudCommand;
import dev.kylejulian.twsmanagement.commands.WhitelistExemptCommand;
import dev.kylejulian.twsmanagement.commands.tabcompleters.AfkTabCompleter;
import dev.kylejulian.twsmanagement.commands.tabcompleters.WhitelistExemptTabCompleter;
import dev.kylejulian.twsmanagement.configuration.*;
import dev.kylejulian.twsmanagement.data.DatabaseConnectionManager;
import dev.kylejulian.twsmanagement.data.MojangApi;
import dev.kylejulian.twsmanagement.data.interfaces.IDatabaseManager;
import dev.kylejulian.twsmanagement.data.interfaces.IExemptDatabaseManager;
import dev.kylejulian.twsmanagement.data.interfaces.IHudDatabaseManager;
import dev.kylejulian.twsmanagement.data.sqlite.AfkDatabaseManager;
import dev.kylejulian.twsmanagement.data.sqlite.HudDatabaseManager;
import dev.kylejulian.twsmanagement.data.sqlite.WhitelistDatabaseManager;
import dev.kylejulian.twsmanagement.player.PlayerListener;
import dev.kylejulian.twsmanagement.player.hud.HudListener;
import dev.kylejulian.twsmanagement.server.whitelist.WhitelistRunnable;
import dev.kylejulian.twsmanagement.world.PlayerEventsListener;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class TWSManagement extends JavaPlugin {

    private final ConfigurationManager configManager;
    private DatabaseConnectionManager databaseConnectionManager;

	public TWSManagement() {
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

		this.databaseConnectionManager = new DatabaseConnectionManager(databaseConfig,
				this.getDataFolder().getAbsolutePath());
		IExemptDatabaseManager afkDatabaseManager = new AfkDatabaseManager(this,
				this.databaseConnectionManager);
		IExemptDatabaseManager whitelistExemptDatabaseManager = new WhitelistDatabaseManager(this,
				this.databaseConnectionManager);
		IHudDatabaseManager hudDatabaseManager = new HudDatabaseManager(this,
				this.databaseConnectionManager);

		this.getLogger().log(Level.INFO, "Internal dependencies have been created by {0}ms", stopWatch.getTime());

		runDefaultSchemaSetup(new IDatabaseManager[] {afkDatabaseManager, hudDatabaseManager,
				whitelistExemptDatabaseManager }, stopWatch);

		this.getServer().getPluginManager().registerEvents(new PlayerListener(this, afkDatabaseManager,
				hudDatabaseManager, this.configManager), this);
		this.getServer().getPluginManager().registerEvents(new AfkEventListener(this, afkConfig),
				this);
		this.getServer().getPluginManager().registerEvents(new PlayerEventsListener(this, nightResetConfig),
				this);
		this.getServer().getPluginManager().registerEvents(new HudListener(this, hudConfig), this);

		this.getLogger().log(Level.INFO, "Plugin Events have been registered by {0}ms", stopWatch.getTime());

		this.getServer().getScheduler().runTaskTimerAsynchronously(this,
				new WhitelistRunnable(this, whitelistConfig, whitelistExemptDatabaseManager),
				0, whitelistConfig.getCheck().getSeconds() * 20); //200

		MojangApi mojangApi = new MojangApi(this.getLogger());
		Objects.requireNonNull(this.getCommand("afk"))
				.setExecutor(new AfkCommand(this, afkDatabaseManager, mojangApi));
		Objects.requireNonNull(this.getCommand("afk"))
				.setTabCompleter(new AfkTabCompleter(this));
		Objects.requireNonNull(this.getCommand("hud"))
				.setExecutor(new HudCommand(this, hudDatabaseManager));
		Objects.requireNonNull(this.getCommand("exe"))
				.setExecutor(new WhitelistExemptCommand(this, whitelistExemptDatabaseManager, mojangApi));
		Objects.requireNonNull(this.getCommand("exe"))
				.setTabCompleter(new WhitelistExemptTabCompleter(this));

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

	private void runDefaultSchemaSetup(@NotNull IDatabaseManager[] databaseManagers, StopWatch stopWatch) {
		for (IDatabaseManager databaseManager : databaseManagers) {
			try {
				databaseManager.setupDefaultSchema().get();
				this.getLogger().log(Level.INFO, "{0} have been verified by {1}ms",
						new Object[] { databaseManager.getClass().getSimpleName(), stopWatch.getTime() });
			} catch (InterruptedException | ExecutionException e) {
				this.getLogger().log(Level.SEVERE,
						"Unable to setup Database Schemas, plugin may not work as expected. Disabling plugin.");
				this.getServer().getPluginManager().disablePlugin(this);
			}
		}
	}
}
