package dev.kylejulian.twsmanagement.commands;

import dev.kylejulian.twsmanagement.afk.events.AfkCommandEvent;
import dev.kylejulian.twsmanagement.commands.models.ExemptFutureModel;
import dev.kylejulian.twsmanagement.data.MojangApi;
import dev.kylejulian.twsmanagement.data.entities.EntityExemptList;
import dev.kylejulian.twsmanagement.data.interfaces.IExemptDatabaseManager;
import dev.kylejulian.twsmanagement.extensions.ExemptListChatHelpers;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class AfkCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final IExemptDatabaseManager afkDatabaseManager;
    private final MojangApi mojangApi;

    public AfkCommand(@NotNull JavaPlugin plugin, @NotNull IExemptDatabaseManager afkDatabaseManager, @NotNull MojangApi mojangApi) {
        this.plugin = plugin;
        this.afkDatabaseManager = afkDatabaseManager;
        this.mojangApi = mojangApi;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length < 1) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                AfkCommandEvent event = new AfkCommandEvent(player.getUniqueId());
                Runnable afkEventTask = () -> this.plugin.getServer().getPluginManager().callEvent(event);

                this.plugin.getServer().getScheduler().runTask(this.plugin, afkEventTask);
            } else {
                this.plugin.getServer().getLogger().log(Level.WARNING, "You must be a Player to use this command!");
            }

            return true;
        } else if (args.length > 1) {
            String base = args[0];
            String command = args[1];

            if (!base.equalsIgnoreCase("exempt")) {
                sender.sendMessage(ChatColor.RED + "Command is not recognised");
                return false;
            }

            if (!sender.hasPermission("tws.afk.exempt") && !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "You do not have permissions to use this command.");
                return true;
            }

            if (command.equalsIgnoreCase("list")) {
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
                int pageSize = 5;
                CompletableFuture<EntityExemptList> getAfkExemptPlayers = this.afkDatabaseManager.getPlayers(pageIndex, pageSize);

                getAfkExemptPlayers
                        .thenAcceptAsync(result -> {
                            ArrayList<UUID> playerIds = result.getPlayerIds();
                            int maxPages = result.getMaxPageCount();

                            if (playerIds.isEmpty()) {
                                sender.sendMessage(ChatColor.YELLOW + "There are no results to be shown.");
                                return;
                            }

                            sender.sendMessage(ChatColor.YELLOW + "AFK Kick Exempt List");
                            ExemptListChatHelpers exemptListChatHelpers = new ExemptListChatHelpers(this.plugin, this.mojangApi);

                            ComponentBuilder baseMessage = exemptListChatHelpers.buildPaginationMessage(finalPageIndex, maxPages, "/afk exempt list", playerIds);
                            sender.spigot().sendMessage(baseMessage.create());

                            if (!(sender instanceof Player) && finalPageIndex != maxPages) {
                                sender.sendMessage(ChatColor.YELLOW + "To fetch the next page you need to use [" + ChatColor.GREEN + "/afk exempt list "
                                        + (finalPageIndex + 1) + ChatColor.YELLOW + "]");
                            }
                        });

                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "You need to specify the correct number of arguments.");
                return false;
            }
            String target = args[2];
            CompletableFuture<UUID> playerIdFuture = this.mojangApi.getPlayerId(target);

            playerIdFuture
                    .thenComposeAsync(afkDatabaseManager::isExempt)
                    .thenCombineAsync(playerIdFuture, (isExempt, uuid) -> new ExemptFutureModel(uuid, isExempt))
                    .thenComposeAsync(whitelistExemptFutureModel -> {
                        if (whitelistExemptFutureModel.getIsExempt()) {
                            // Exempt
                            if (command.equalsIgnoreCase("add")) {
                                this.plugin.getServer().getScheduler().runTask(this.plugin,
                                        () -> sender.sendMessage(ChatColor.RED + target + " is already AFK Kick exempt"));
                                return new CompletableFuture<>();
                            }

                            this.plugin.getServer().getScheduler().runTask(this.plugin,
                                    () -> sender.sendMessage(ChatColor.GREEN + target + " was removed from the AFK Kick exempt list"));
                            return afkDatabaseManager.remove(whitelistExemptFutureModel.getPlayerId());
                        } else {
                            // Not exempt
                            if (command.equalsIgnoreCase("add")) {
                                this.plugin.getServer().getScheduler().runTask(this.plugin,
                                        () -> sender.sendMessage(ChatColor.GREEN + target + " was added to AFK Kick exempt list"));
                                return afkDatabaseManager.add(whitelistExemptFutureModel.getPlayerId());
                            }

                            this.plugin.getServer().getScheduler().runTask(this.plugin,
                                    () -> sender.sendMessage(ChatColor.RED + target + " is not AFK Kick exempt"));
                            return new CompletableFuture<>();
                        }
                    });
        }

        return true;
    }
}
