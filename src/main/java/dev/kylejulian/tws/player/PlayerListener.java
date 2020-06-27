package dev.kylejulian.tws.player;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import dev.kylejulian.tws.data.interfaces.IAfkDatabaseManager;
import dev.kylejulian.tws.data.interfaces.IHudDatabaseManager;
import dev.kylejulian.tws.player.hud.events.HudEvent;
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
import dev.kylejulian.tws.extensions.TabPluginHelper;
import org.jetbrains.annotations.NotNull;

public class PlayerListener implements Listener {

	private final JavaPlugin plugin;
	private final IAfkDatabaseManager afkDatabaseManager;
	private final IHudDatabaseManager hudDatabaseManager;
	private final ConfigurationManager configManager;
	private final HashMap<UUID,Integer> playerAfkManagerTasks;
	
	public PlayerListener(@NotNull JavaPlugin plugin, @NotNull IAfkDatabaseManager afkDatabaseManager,
						  @NotNull IHudDatabaseManager hudDatabaseManager, @NotNull ConfigurationManager configManager) {
		this.plugin = plugin;
		this.afkDatabaseManager = afkDatabaseManager;
		this.hudDatabaseManager = hudDatabaseManager;
		this.configManager = configManager;
		this.playerAfkManagerTasks = new HashMap<>();
	}
	
	@EventHandler
	public void onJoin(@NotNull PlayerJoinEvent e) {
		Player player = e.getPlayer();
		final UUID playerId = player.getUniqueId();
		Integer taskId = this.createAndStartAfkManagerTask(playerId);

		this.playerAfkManagerTasks.put(playerId, taskId);

		CompletableFuture<Boolean> isHudEnabledFuture = this.hudDatabaseManager.isEnabled(playerId);
		CompletableFuture<Void> raiseHudEventFuture = isHudEnabledFuture.thenAcceptAsync(result -> {
			if (result) {
				// Raise Hud Event synchronously
				plugin.getServer().getScheduler().runTask(plugin, () ->
						plugin.getServer().getPluginManager().callEvent(new HudEvent(playerId, true)));
			}
		});
		raiseHudEventFuture.join();
	}

	@EventHandler
	public void onLeave(@NotNull PlayerQuitEvent e) {
		Player player = e.getPlayer();
		final UUID playerId = player.getUniqueId();
		Integer taskId = this.playerAfkManagerTasks.getOrDefault(playerId, null);

		if (taskId != null) {
			this.plugin.getServer().getScheduler().cancelTask(taskId);
		}	
	}
	
	@EventHandler
	public void onAfkCancelled(@NotNull AfkCancelledEvent e) {
		final UUID playerId = e.getPlayerId();
		Integer taskId = this.playerAfkManagerTasks.getOrDefault(playerId, null);

		if (taskId != null) { // In the event of a reload
			this.plugin.getServer().getScheduler().cancelTask(taskId);
		}
		
		Runnable tabTask = () -> TabPluginHelper.setTabSuffix(this.plugin, playerId, ChatColor.RESET + "");
		this.plugin.getServer().getScheduler().runTask(this.plugin, tabTask);

		taskId = this.createAndStartAfkManagerTask(playerId);
		this.playerAfkManagerTasks.put(playerId, taskId);
	}
	
	@EventHandler
	public void onAfkCommand(@NotNull AfkCommandEvent e) {
		UUID playerId = e.getPlayerId();
		Integer taskId = this.playerAfkManagerTasks.getOrDefault(playerId, null);
		
		if (taskId != null) { // In the event of a reload
			this.plugin.getServer().getScheduler().cancelTask(taskId);
		}

		taskId = this.createAndStartAfkManagerTask(playerId, true); // Player triggered this AFK event, they are already AFK
		this.playerAfkManagerTasks.put(playerId, taskId);
		
		// Raise new AFK Event, as the AFKManager will not raise another due to the alreadyAfk being set to true property
		AfkEvent event = new AfkEvent(playerId);
		Runnable afkEventTask = () -> this.plugin.getServer().getPluginManager().callEvent(event);
		this.plugin.getServer().getScheduler().runTask(this.plugin, afkEventTask); // Cannot raise a new event asynchronously
	}
	
	@EventHandler
	public void onAfk(@NotNull AfkEvent e) {
		UUID playerId = e.getPlayerId();
		
		Runnable tabTask = () -> TabPluginHelper.setTabSuffix(this.plugin, playerId, ChatColor.GRAY + "[" + ChatColor.RED + "AFK" + ChatColor.GRAY + "] " + ChatColor.RESET);
		this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, tabTask);
	}
	
	private Integer createAndStartAfkManagerTask(@NotNull UUID playerId) {
		return createAndStartAfkManagerTask(playerId, false);
	}
	
	private Integer createAndStartAfkManagerTask(@NotNull UUID playerId, boolean alreadyAfk) {
		ConfigModel config = this.configManager.getConfig();
		AfkConfigModel afkConfig = config.getAfkConfig();
		
		Runnable playerAfkManager = new AfkManager(this.plugin, this.afkDatabaseManager, afkConfig, playerId, alreadyAfk);
		BukkitTask afkTimerTask = this.plugin.getServer().getScheduler()
				.runTaskTimerAsynchronously(this.plugin, playerAfkManager, 1200, 1200); // 1200 ticks = 60 seconds
		
		return afkTimerTask.getTaskId();
	}
}
