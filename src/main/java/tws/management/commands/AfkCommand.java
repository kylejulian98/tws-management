package tws.management.commands;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import tws.management.afk.events.AfkCommandEvent;
import tws.management.data.AfkDatabaseManager;
import tws.management.data.MojangApi;
import tws.management.data.QueryCallback;

public class AfkCommand implements CommandExecutor {

	private final JavaPlugin plugin;
	private final AfkDatabaseManager afkDatabaseManager;
	private final MojangApi mojangApi;

	public AfkCommand(JavaPlugin plugin, AfkDatabaseManager afkDatabaseManager, MojangApi mojangApi) {
		this.plugin = plugin;
		this.afkDatabaseManager = afkDatabaseManager;
		this.mojangApi = mojangApi;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length < 1) {
			if (sender instanceof Player) {
				Player player = (Player) sender;

				AfkCommandEvent event = new AfkCommandEvent(player.getUniqueId());
				Runnable afkEventTask = () -> {
					player.sendMessage(ChatColor.DARK_RED + "You are now AFK");
					this.plugin.getServer().getPluginManager().callEvent(event);
				};

				this.plugin.getServer().getScheduler().runTask(this.plugin, afkEventTask);
			}
		}

		if (args.length > 2) {
			String base = args[0];
			String command = args[1];
			String target = args[2];
			if (sender.hasPermission("tws.afk.exempt") || sender.isOp()) {
				if (base.equalsIgnoreCase("exempt")) {
					UUID playerId;

					try {
						playerId = this.mojangApi.getPlayerId(target);
					} catch (Exception e) {
						sender.sendMessage(ChatColor.RED + "You need to specify a target player to exempt.");
						this.plugin.getLogger().log(Level.WARNING, e.getMessage());
						return true;
					}

					if (command.equalsIgnoreCase("add")) {
						this.afkDatabaseManager.isPlayerKickExempt(playerId, new QueryCallback() {
							@Override
							public void onQueryComplete(boolean result) {
								if (result) { // Player is exempt
									queueSendMessageSync(sender, ChatColor.RED + "Player [" + ChatColor.BLUE + target
											+ ChatColor.RED + "] is already exempt.");
									return;
								}

								afkDatabaseManager.addPlayerToKickExempt(playerId, null);
								queueSendMessageSync(sender, ChatColor.GREEN + "Added [" + ChatColor.BLUE + target
										+ ChatColor.GREEN + "] to the Afk Kick exempt list.");
							}
						});
					} else if (command.equalsIgnoreCase("remove")) {
						this.afkDatabaseManager.isPlayerKickExempt(playerId, new QueryCallback() {
							@Override
							public void onQueryComplete(boolean result) {
								if (result) { // Player is exempt
									afkDatabaseManager.removePlayerFromKickExempt(playerId, null);
									queueSendMessageSync(sender, ChatColor.GREEN + "Player [" + ChatColor.BLUE + target
											+ ChatColor.GREEN + "] has been removed from the Afk Kick exempt list.");
									return;
								}

								queueSendMessageSync(sender, ChatColor.RED + "Player [" + ChatColor.BLUE + target
										+ ChatColor.RED + "] is not exempt.");
							}
						});
					} else {
						sender.sendMessage(ChatColor.RED + "Command not recognised.");
					}
				}
			} else {
				sender.sendMessage(ChatColor.RED + "You do not have permissions to use this command.");
				return true;
			}
		} else {
			sender.sendMessage(ChatColor.RED + "You need to specify the correct number of arguments.");
			return false;
		}

		return true;
	}

	private void queueSendMessageSync(CommandSender sender, String message) {
		this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
			sender.sendMessage(message);
		});
	}
}
