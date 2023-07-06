package dev.kylejulian.twsmanagement.player;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import dev.kylejulian.twsmanagement.data.interfaces.IExemptDatabaseManager;
import dev.kylejulian.twsmanagement.data.interfaces.IHudDatabaseManager;
import dev.kylejulian.twsmanagement.player.hud.events.HudEvent;
import dev.kylejulian.twsmanagement.afk.AfkManager;
import dev.kylejulian.twsmanagement.afk.events.AfkCancelledEvent;
import dev.kylejulian.twsmanagement.afk.events.AfkEvent;
import dev.kylejulian.twsmanagement.configuration.AfkConfigModel;
import dev.kylejulian.twsmanagement.extensions.TabPluginHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import dev.kylejulian.twsmanagement.afk.events.AfkCommandEvent;
import dev.kylejulian.twsmanagement.configuration.ConfigModel;
import dev.kylejulian.twsmanagement.configuration.ConfigurationManager;
import org.jetbrains.annotations.NotNull;

public class PlayerListener implements Listener {

	final TextComponent afkText = Component.text()
		.color(NamedTextColor.GRAY)
		.appendSpace()
		.append(Component.text("["))
		.color(NamedTextColor.RED)
		.append(Component.text("AFK"))
		.color(NamedTextColor.GRAY)
		.append(Component.text("]"))
		.style(Style.empty())
		.build();

	private final JavaPlugin plugin;
	private final IExemptDatabaseManager afkDatabaseManager;
	private final IHudDatabaseManager hudDatabaseManager;
	private final ConfigurationManager configManager;
	private final HashMap<UUID,Integer> playerAfkManagerTasks;
	
	public PlayerListener(@NotNull JavaPlugin plugin,
						  @NotNull IExemptDatabaseManager afkDatabaseManager,
						  @NotNull IHudDatabaseManager hudDatabaseManager,
						  @NotNull ConfigurationManager configManager) {
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
		isHudEnabledFuture.thenAcceptAsync(result -> {
			if (result) {
				// Raise Hud Event synchronously
				plugin.getServer().getScheduler().runTask(plugin, () ->
						plugin.getServer().getPluginManager().callEvent(new HudEvent(playerId, true)));
			}
		});
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

		// Only reset the Tab if they were actually AFK
		if (TabPluginHelper.hasTabSuffix(playerId)) {
			Runnable tabTask = () -> TabPluginHelper.resetTabSuffix(playerId);
			this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, tabTask);
		}

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

		// Player triggered this AFK event, they are already AFK
		taskId = this.createAndStartAfkManagerTask(playerId, true);
		this.playerAfkManagerTasks.put(playerId, taskId);
		
		// Raise new AFK Event, as the AFKManager will not raise another due to the
		// alreadyAfk being set to true property
		// This is so the Plugin will kick the Player after the configurable kick time has elapsed
		AfkEvent event = new AfkEvent(playerId);
		Runnable afkEventTask = () -> this.plugin.getServer().getPluginManager().callEvent(event);
		// Cannot raise a new event asynchronously
		this.plugin.getServer().getScheduler().runTask(this.plugin, afkEventTask);
	}
	
	@EventHandler
	public void onAfk(@NotNull AfkEvent e) {
		

		UUID playerId = e.getPlayerId();

		if (!TabPluginHelper.hasTabSuffix(playerId)) {
			Player player = this.plugin.getServer().getPlayer(playerId);
			if (player != null) {
				AfkConfigModel afkConfig = configManager.getConfig().getAfkConfig();
				if (afkConfig.getSendPlayerAfkMessage()) {
					TextComponent youAreAfk = Component.text()
						.color(NamedTextColor.DARK_RED)
						.append(Component.text("You are now AFK"))
						.build();

					player.sendMessage(youAreAfk);
				}

				Runnable tabTask = () -> TabPluginHelper.setTabSuffix(playerId, afkText);
				this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, tabTask);
			}
		}
	}

	/**
	 * Creates and Starts a Afk Manager task, which determines when a Player is actually afk
	 * @param playerId Player to create the Afk Manager task for
	 * @return Task Id
	 */
	private int createAndStartAfkManagerTask(@NotNull UUID playerId) {
		return createAndStartAfkManagerTask(playerId, false);
	}

	/**
	 * Creates and Starts a Afk Manager task, which determines when a Player is actually afk. This overload
	 * providers a boolean parameter which allows you to specify if the Player had requested the event, through /afk
	 * @param playerId Player to create the Afk Manager task for
	 * @param alreadyAfk Flag whether or not the Player has request to be Afk
	 * @return Task Id
	 */
	private int createAndStartAfkManagerTask(@NotNull UUID playerId, boolean alreadyAfk) {
		ConfigModel config = this.configManager.getConfig();
		AfkConfigModel afkConfig = config.getAfkConfig();
		
		Runnable playerAfkManager =
				new AfkManager(this.plugin, this.afkDatabaseManager, afkConfig, playerId, alreadyAfk);

		// 1200 ticks = 60 seconds
		BukkitTask afkTimerTask = this.plugin.getServer().getScheduler()
				.runTaskTimerAsynchronously(this.plugin, playerAfkManager, 1200, 1200);
		
		return afkTimerTask.getTaskId();
	}
}
