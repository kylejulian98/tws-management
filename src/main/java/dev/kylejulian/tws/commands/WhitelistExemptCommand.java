package dev.kylejulian.tws.commands;

import dev.kylejulian.tws.commands.models.WhitelistExemptFutureModel;
import dev.kylejulian.tws.data.MojangApi;
import dev.kylejulian.tws.data.entities.EntityExemptList;
import dev.kylejulian.tws.data.interfaces.IExemptDatabaseManager;
import dev.kylejulian.tws.extensions.ExemptListChatHelpers;
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

public class WhitelistExemptCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final IExemptDatabaseManager whitelistExemptDatabaseManager;
    private final MojangApi mojangApi;

    public WhitelistExemptCommand(@NotNull JavaPlugin plugin, @NotNull IExemptDatabaseManager whitelistExemptDatabaseManager, @NotNull MojangApi mojangApi) {
        this.plugin = plugin;
        this.whitelistExemptDatabaseManager = whitelistExemptDatabaseManager;
        this.mojangApi = mojangApi;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "You need to specify a command add/remove/list");
            return true;
        }

        String command = args[0];
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
            final int pageSize = 5;

            CompletableFuture<EntityExemptList> getAutoUnwhitelistExemptPlayers = this.whitelistExemptDatabaseManager.getPlayers(finalPageIndex, pageSize);

            getAutoUnwhitelistExemptPlayers.thenAcceptAsync(result -> {
                ArrayList<UUID> playerIds = result.getPlayerIds();
                int maxPages = result.getMaxPageCount();

                if (playerIds.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "There are no results to be shown.");
                    return;
                }

                sender.sendMessage(ChatColor.YELLOW + "Auto Unwhitelist Exempt List");
                ExemptListChatHelpers exemptListChatHelpers = new ExemptListChatHelpers(this.plugin, this.mojangApi);

                ComponentBuilder baseMessage = exemptListChatHelpers.buildPaginationMessage(finalPageIndex, maxPages, "/exe list", playerIds);
                sender.spigot().sendMessage(baseMessage.create());

                if (!(sender instanceof Player) && finalPageIndex != maxPages) {
                    sender.sendMessage(ChatColor.YELLOW + "To fetch the next page you need to use [" + ChatColor.GREEN + "/exe list " +
                            (finalPageIndex + 1) + ChatColor.YELLOW + "]");
                }
            });

            return true;
        } else {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You must specify a player");
                return true;
            }

            if (!sender.isOp() && !sender.hasPermission("tws.exempt")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command");
                return true;
            }

            String target = args[1];
            CompletableFuture<UUID> playerIdFuture = this.mojangApi.getPlayerId(target);

            playerIdFuture
                    .thenComposeAsync(whitelistExemptDatabaseManager::isExempt)
                    .thenCombineAsync(playerIdFuture, (isExempt, uuid) -> new WhitelistExemptFutureModel(uuid, isExempt))
                    .thenComposeAsync(whitelistExemptFutureModel -> {
                        if (whitelistExemptFutureModel.getIsExempt()) {
                            // Exempt
                            if (command.equalsIgnoreCase("add")) {
                                this.plugin.getServer().getScheduler().runTask(this.plugin,
                                        () -> sender.sendMessage(ChatColor.RED + target + " is already auto unwhitelist exempt"));
                                return new CompletableFuture<>();
                            }

                            this.plugin.getServer().getScheduler().runTask(this.plugin,
                                    () -> sender.sendMessage(ChatColor.GREEN + target + " was removed from the auto unwhitelist exempt list"));
                            return whitelistExemptDatabaseManager.remove(whitelistExemptFutureModel.getPlayerId());
                        } else {
                            // Not exempt
                            if (command.equalsIgnoreCase("add")) {
                                this.plugin.getServer().getScheduler().runTask(this.plugin,
                                        () -> sender.sendMessage(ChatColor.GREEN + target + " was added to auto unwhitelist exempt list"));
                                return whitelistExemptDatabaseManager.add(whitelistExemptFutureModel.getPlayerId());
                            }

                            this.plugin.getServer().getScheduler().runTask(this.plugin,
                                    () -> sender.sendMessage(ChatColor.RED + target + " is not auto unwhitelist exempt"));
                            return new CompletableFuture<>();
                        }
                    });
        }


        return true;
    }
}

