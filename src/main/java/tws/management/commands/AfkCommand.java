package tws.management.commands;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import tws.management.afk.events.AfkCommandEvent;
import tws.management.data.AfkDatabaseManager;
import tws.management.data.MojangApi;
import tws.management.data.callbacks.AfkKickExemptListQueryCallback;
import tws.management.data.callbacks.BooleanQueryCallback;
import tws.management.data.entities.AfkKickExemptList;

public class AfkCommand implements CommandExecutor {

	private final JavaPlugin plugin;
	private final AfkDatabaseManager afkDatabaseManager;
	private final MojangApi mojangApi;

	private final int pageSize = 5;

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
			} else {
				this.plugin.getServer().getLogger().log(Level.WARNING, "You must be a Player to use this command!");
			}

			return true;
		} else if (args.length > 1) {
			String base = args[0];
			String command = args[1];

			if (sender.hasPermission("tws.afk.exempt") || sender.isOp()) {
				if (base.equalsIgnoreCase("exempt")) {
					if (command.equalsIgnoreCase("add")) {
						UUID playerId = null;
						String target;
						if (args.length == 3) {
							target = args[2];
							playerId = this.getPlayerIdFromMojangApi(target);
						} else {
							return false;
						}

						if (playerId == null) {
							sender.sendMessage(ChatColor.RED + "You need to specify a target player to exempt.");
							return true;
						}

						final UUID finalPlayerId = playerId;
						this.afkDatabaseManager.isPlayerKickExempt(finalPlayerId, new BooleanQueryCallback() {
							@Override
							public void onQueryComplete(Boolean result) {
								if (result) { // Player is exempt
									queueSendMessageSync(sender, ChatColor.RED + "Player [" + ChatColor.BLUE + target + ChatColor.RED + "] is already exempt.");
									return;
								}

								afkDatabaseManager.addPlayerToKickExempt(finalPlayerId, null);
								queueSendMessageSync(sender,
										ChatColor.GREEN + "Added [" + ChatColor.BLUE + target + ChatColor.GREEN + "] to the Afk Kick exempt list.");
							}
						});
					} else if (command.equalsIgnoreCase("remove")) {
						UUID playerId = null;
						String target;
						if (args.length == 3) {
							target = args[2];
							playerId = this.getPlayerIdFromMojangApi(target);
						} else {
							return false;
						}

						if (playerId == null) {
							sender.sendMessage(ChatColor.RED + "You need to specify a target player to exempt.");
							return true;
						}

						final UUID finalPlayerId = playerId;
						this.afkDatabaseManager.isPlayerKickExempt(playerId, new BooleanQueryCallback() {
							@Override
							public void onQueryComplete(Boolean result) {
								if (result) { // Player is exempt
									afkDatabaseManager.removePlayerFromKickExempt(finalPlayerId, null);
									queueSendMessageSync(sender, ChatColor.GREEN + "Player [" + ChatColor.BLUE + target + ChatColor.GREEN
											+ "] has been removed from the Afk Kick exempt list.");
									return;
								}

								queueSendMessageSync(sender, ChatColor.RED + "Player [" + ChatColor.BLUE + target + ChatColor.RED + "] is not exempt.");
							}
						});
					} else if (command.equalsIgnoreCase("list")) {
						int pageIndex;

						if (args.length == 3) {
							try {
								pageIndex = Integer.parseInt(args[2]);
								if (pageIndex < 1) {
									pageIndex = 1;
								}
							} catch (NumberFormatException e) {
								sender.sendMessage(ChatColor.RED + "You must specify a valid page number!");
								return false;
							}
						} else {
							pageIndex = 1;
						}

						final int finalPageIndex = pageIndex;
						this.afkDatabaseManager.getAfkKickExemptPlayers(pageIndex, this.pageSize, new AfkKickExemptListQueryCallback() {
							@Override
							public void onQueryComplete(AfkKickExemptList result) {
								ArrayList<UUID> playerIds = result.getPlayerIds();
								int maxPages = result.getPageCount();

								if (playerIds.isEmpty()) {
									sender.sendMessage(ChatColor.YELLOW + "There are no results to be shown.");
									return;
								}

								sender.sendMessage(ChatColor.YELLOW + "AFK Kick Exempt List");
								ComponentBuilder baseMessage = buildPaginationMessage(finalPageIndex, maxPages, playerIds);
								sender.spigot().sendMessage(baseMessage.create());

								if (!(sender instanceof Player) && finalPageIndex != maxPages) {
									sender.sendMessage(ChatColor.YELLOW + "To fetch the next page you need to use [" + ChatColor.GREEN + "/afk exempt list "
											+ (finalPageIndex + 1) + ChatColor.YELLOW + "]");
								}
							}

						});
					} else {
						sender.sendMessage(ChatColor.RED + "Command not recognised.");
					}
				}
			} else {
				sender.sendMessage(ChatColor.RED + "You do not have permissions to use this command.");
			}
		} else {
			sender.sendMessage(ChatColor.RED + "You need to specify the correct number of arguments.");
		}

		return true;
	}
	
	private ComponentBuilder buildPaginationMessage(int pageIndex, int maxPages, ArrayList<UUID> playerIds) {
		int nextPage = pageIndex + 1;
		int prevPage = pageIndex - 1;

		TextComponent newLine = new TextComponent("\n");
		ComponentBuilder baseMessage = new ComponentBuilder();

		baseMessage.append(newLine);

		for (UUID id : playerIds) {
			String playerName = null;
			Player player = this.plugin.getServer().getPlayer(id);
			
			// If player is not online, fetch it from the Mojang API
			if (player == null) {
				playerName = this.getNameFromMojangApi(id);
			}
			else {
				playerName = player.getDisplayName();
			}

			if (playerName == null || playerName == "") {
				playerName = id.toString();
			}
			
			baseMessage.append(ChatColor.YELLOW + "Player (" + ChatColor.GREEN + playerName + ChatColor.YELLOW + ")").append(newLine);
		}

		baseMessage.append(newLine).append(ChatColor.RED + "<--" + ChatColor.RESET);

		if (prevPage > 0) { // Only allow user to go back if they can
			BaseComponent[] previousPageHoverText = new ComponentBuilder("Click to go to the previous Page").create();
			HoverEvent previousPageHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, previousPageHoverText);
			baseMessage.event(previousPageHoverEvent).event(new ClickEvent(Action.RUN_COMMAND, "/afk exempt list " + prevPage));
			baseMessage.append("").reset(); // Fix to prevent the rest of the baseMessage being associated to this event.
		}
		

		baseMessage.append(ChatColor.YELLOW + " Page (" + ChatColor.GREEN + pageIndex + "/" + maxPages + ChatColor.YELLOW + ") " + ChatColor.RESET)
				.append(ChatColor.RED + "-->" + ChatColor.RESET);

		if (nextPage <= maxPages) {// Only allow user to go forward if they can
			BaseComponent[] nextPageHoverText = new ComponentBuilder("Click to go to the next Page").create();
			HoverEvent nextPageHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, nextPageHoverText);
			baseMessage.event(nextPageHoverEvent).event(new ClickEvent(Action.RUN_COMMAND, "/afk exempt list " + nextPage));
		}
		baseMessage.append(newLine);

		return baseMessage;
	}

	private UUID getPlayerIdFromMojangApi(String name) {
		UUID playerId = null;

		try {
			playerId = this.mojangApi.getPlayerId(name);
		} catch (Exception e) {
			this.plugin.getLogger().log(Level.WARNING, "Unable to fetch player Id for Player [" + name + "]");
			this.plugin.getLogger().log(Level.WARNING, e.getMessage());
		}

		return playerId;
	}
	
	private String getNameFromMojangApi(UUID playerId) {
		String playerName = null;

		try {
			playerName = this.mojangApi.getPlayerName(playerId);
		} catch (Exception e) {
			this.plugin.getLogger().log(Level.WARNING, "Unable to fetch player name for Player [" + playerId + "]");
			this.plugin.getLogger().log(Level.WARNING, e.getMessage());
		}

		return playerName;
	}

	private void queueSendMessageSync(CommandSender sender, String message) {
		this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
			sender.sendMessage(message);
		});
	}
}
