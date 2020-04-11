package tws.management.player;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import tws.management.afk.AfkManager;
import tws.management.afk.events.AfkCancelledEvent;
import tws.management.afk.events.AfkCommandEvent;
import tws.management.configuration.AfkConfigModel;
import tws.management.configuration.ConfigModel;
import tws.management.configuration.ConfigurationManager;
import tws.management.data.AfkDatabaseManager;

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
		
		afkTimerTask = this.createAndStartAfkManagerTask(playerId);
		this.playerAfkManagerTasks.put(playerId, afkTimerTask);
	}
	
	@EventHandler
	public void onAfk(AfkCommandEvent e) {
		UUID playerId = e.getPlayerId();
		BukkitTask afkTimerTask = this.playerAfkManagerTasks.getOrDefault(playerId, null);
		
		if (afkTimerTask != null) { // In the event of a reload
			afkTimerTask.cancel();
		}
		
		afkTimerTask = this.createAndStartAfkManagerTask(playerId, true); // Player triggered this AFK event, they are already AFK
		this.playerAfkManagerTasks.put(playerId, afkTimerTask);
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
