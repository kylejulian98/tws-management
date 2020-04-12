package tws.management.afk;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import tws.management.afk.events.AfkEvent;
import tws.management.configuration.AfkConfigModel;
import tws.management.data.AfkDatabaseManager;
import tws.management.data.callbacks.BooleanQueryCallback;

/**
 * 
 * Manages the AFK for a given player
 *
 */
public class AfkManager extends BukkitRunnable {

	private int afkMinutes;
	private final boolean alreadyAfk;
	private final JavaPlugin plugin;
	private final AfkDatabaseManager afkDatabase;
	private final AfkConfigModel afkConfig;
	private final UUID playerId;
	
	/**
	 * Creates a new AfkManager for a given Player
	 * @param plugin JavaPlugin
	 * @param afkDatabase AFK Database Manager
	 * @param afkConfig Configuration for AFK
	 * @param playerId Player whom this AFK Manager belongs to
	 */
	public AfkManager(JavaPlugin plugin, AfkDatabaseManager afkDatabase, AfkConfigModel afkConfig, UUID playerId) {
		this(plugin, afkDatabase, afkConfig, playerId, false);
	}
	
	/**
	 * Creates a new AfkManager for a given Player
	 * @param plugin JavaPlugin
	 * @param afkDatabase AFK Database Manager
	 * @param afkConfig Configuration for AFK
	 * @param playerId Player whom this AFK Manager belongs to
	 * @param alreadyAfk Boolean flag to indicate whether or not the Player was already AFK when this AFK Manager was created
	 */
	public AfkManager(JavaPlugin plugin, AfkDatabaseManager afkDatabase, AfkConfigModel afkConfig, UUID playerId, boolean alreadyAfk) {
		this.plugin = plugin;
		this.afkDatabase = afkDatabase;
		this.afkConfig = afkConfig;
		this.playerId = playerId;
		this.alreadyAfk = alreadyAfk;
	}
	
	@Override
	public void run() {
		int afkTime = this.afkConfig.getTimeMinutes();
		int afkKickTime = this.afkConfig.getKickTimeMinutes();
		
		Player player = this.plugin.getServer().getPlayer(this.playerId);
		if (player == null) { // Player does not exist? Should not be possible since this Runnable would be cancelled
			return;
		}
		
		this.addPlayerAfkMinutes(1);
		
		if (!this.alreadyAfk && this.getPlayerAfkMinutes() == afkTime) { // Player is AFK
			AfkEvent event = new AfkEvent(this.playerId);
			
			this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
				player.sendMessage(ChatColor.DARK_RED + "You are now AFK");
				this.plugin.getServer().getPluginManager().callEvent(event);
			});
		}
		
		BooleanQueryCallback callback = new BooleanQueryCallback() {
			@Override
			public void onQueryComplete(Boolean result) {
				if (!result) { // Player is not Kick exempt
					if (canKickPlayer(player, afkKickTime, afkTime)) {
						player.kickPlayer(afkConfig.getKickMessage());
					}
				}
			}
		};
		
		this.afkDatabase.isPlayerKickExempt(playerId, callback);
	}
	
	private boolean canKickPlayer(Player player, int afkKickTime, int afkTime) {
		if (this.getPlayerAfkMinutes() >= (afkKickTime + afkTime)) { // Player has to be inactive for both the AFK time and AFK Kick time
			int numberOfPlayersOnline = this.plugin.getServer().getOnlinePlayers().size(); 
			int numberOfRequiredPlayersToKick = this.afkConfig.getPlayerCountNeededForKick();
			
			if (numberOfPlayersOnline >= numberOfRequiredPlayersToKick) {
				return true;
			}
		}
		
		return false;
	}

	private int getPlayerAfkMinutes() {
		return this.afkMinutes;
	}
	
	private void addPlayerAfkMinutes(int minutes) {
		this.afkMinutes = this.afkMinutes + minutes;
	}
}
