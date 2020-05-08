package dev.kylejulian.tws.player;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import dev.kylejulian.tws.afk.AfkManager;
import dev.kylejulian.tws.afk.events.AfkCancelledEvent;
import dev.kylejulian.tws.afk.events.AfkCommandEvent;
import dev.kylejulian.tws.afk.events.AfkEvent;
import dev.kylejulian.tws.configuration.AfkConfigModel;
import dev.kylejulian.tws.configuration.ConfigModel;
import dev.kylejulian.tws.configuration.ConfigurationManager;
import dev.kylejulian.tws.data.AfkDatabaseManager;
import dev.kylejulian.tws.extensions.TabPluginHelper;

public class PlayerListener implements Listener {

	private final JavaPlugin plugin;
	private final AfkDatabaseManager afkDatabase;
	private final ConfigurationManager configManager;
	private final HashMap<UUID,BukkitTask> playerAfkManagerTasks;
	
	public PlayerListener(JavaPlugin plugin, AfkDatabaseManager afkDatabase, ConfigurationManager configManager) {
		this.plugin = plugin;
		this.afkDatabase = afkDatabase;
		this.configManager = configManager;
		this.playerAfkManagerTasks = new HashMap<>();
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		UUID playerId = player.getUniqueId();
		BukkitTask afkTimerTask = this.createAndStartAfkManagerTask(playerId);
		
		this.playerAfkManagerTasks.put(playerId, afkTimerTask);
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Player player = e.getPlayer();
		BukkitTask afkManagerTask = this.playerAfkManagerTasks.getOrDefault(player.getUniqueId(), null);
		
		if (afkManagerTask != null) {
			afkManagerTask.cancel();
		}	
	}
	
	@EventHandler
	public void onAfkCancelled(AfkCancelledEvent e) {
		UUID playerId = e.getPlayerId();
		BukkitTask afkTimerTask = this.playerAfkManagerTasks.getOrDefault(playerId, null);
		
		if (afkTimerTask != null) { // In the event of a reload
			afkTimerTask.cancel();
		}
		
		Runnable tabTask = () -> {
			TabPluginHelper.setTabSuffix(this.plugin, playerId, ChatColor.RESET + "");
		};
		this.plugin.getServer().getScheduler().runTask(this.plugin, tabTask);
		
		afkTimerTask = this.createAndStartAfkManagerTask(playerId);
		this.playerAfkManagerTasks.put(playerId, afkTimerTask);
	}
	
	@EventHandler
	public void onAfkCommand(AfkCommandEvent e) {
		UUID playerId = e.getPlayerId();
		BukkitTask afkTimerTask = this.playerAfkManagerTasks.getOrDefault(playerId, null);
		
		if (afkTimerTask != null) { // In the event of a reload
			afkTimerTask.cancel();
		}
		
		afkTimerTask = this.createAndStartAfkManagerTask(playerId, true); // Player triggered this AFK event, they are already AFK
		this.playerAfkManagerTasks.put(playerId, afkTimerTask);
		
		// Raise new AFK Event, as the AFKManager will not raise another due to the alreadyAfk being set to true property
		AfkEvent event = new AfkEvent(playerId);
		Runnable afkEventTask = () -> {
			this.plugin.getServer().getPluginManager().callEvent(event);
		};
		this.plugin.getServer().getScheduler().runTask(this.plugin, afkEventTask); // Cannot raise a new event asynchronously
	}
	
	@EventHandler
	public void onAfk(AfkEvent e) {
		UUID playerId = e.getPlayerId();
		
		Runnable tabTask = () -> {
			TabPluginHelper.setTabSuffix(this.plugin, playerId, ChatColor.GRAY + "[" + ChatColor.RED + "AFK" + ChatColor.GRAY + "] " + ChatColor.RESET);
		};
		this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, tabTask);
	}
	
	private BukkitTask createAndStartAfkManagerTask(UUID playerId) {
		return createAndStartAfkManagerTask(playerId, false);
	}
	
	private BukkitTask createAndStartAfkManagerTask(UUID playerId, boolean alreadyAfk) {
		ConfigModel config = this.configManager.getConfig();
		AfkConfigModel afkConfig = config.getAfkConfig();
		
		AfkManager playerAfkManager = new AfkManager(this.plugin, this.afkDatabase, afkConfig, playerId, alreadyAfk);
		BukkitTask afkTimerTask = playerAfkManager
				.runTaskTimerAsynchronously(plugin, 1200, 1200); // 1200 ticks = 60 seconds
		
		return afkTimerTask;
	}
}
