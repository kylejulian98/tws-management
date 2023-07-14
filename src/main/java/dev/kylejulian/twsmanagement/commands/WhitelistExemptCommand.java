package dev.kylejulian.twsmanagement.commands;

import dev.kylejulian.twsmanagement.commands.models.ExemptFutureModel;
import dev.kylejulian.twsmanagement.data.MojangApi;
import dev.kylejulian.twsmanagement.data.entities.EntityExemptList;
import dev.kylejulian.twsmanagement.data.interfaces.IExemptDatabaseManager;
import dev.kylejulian.twsmanagement.extensions.ExemptListChatHelpers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record WhitelistExemptCommand(JavaPlugin plugin,
                                     IExemptDatabaseManager whitelistExemptDatabaseManager,
                                     MojangApi mojangApi) implements CommandExecutor {

    public WhitelistExemptCommand(@NotNull JavaPlugin plugin,
                                  @NotNull IExemptDatabaseManager whitelistExemptDatabaseManager,
                                  @NotNull MojangApi mojangApi) {
        this.plugin = plugin;
        this.whitelistExemptDatabaseManager = whitelistExemptDatabaseManager;
        this.mojangApi = mojangApi;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command cmd,
                             @NotNull String label,
                             String[] args) {
        if (args.length < 1) {
            TextComponent mustSpecifyValidCommand = Component.text()
                .color(NamedTextColor.RED)
                .append(Component.text("You need to specify a command add/remove/list"))
                .build();

            sender.sendMessage(mustSpecifyValidCommand);
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
                    TextComponent mustSpecifyValidPageNumber = Component.text()
                        .color(NamedTextColor.RED)
                        .append(Component.text("You must specify a valid page number!"))
                        .build();

                    sender.sendMessage(mustSpecifyValidPageNumber);
                    return false;
                }
            } else {
                pageIndex = 1;
            }

            final int finalPageIndex = pageIndex;
            final int pageSize = 5;

            CompletableFuture<EntityExemptList> getAutoUnwhitelistExemptPlayers =
                    this.whitelistExemptDatabaseManager.getPlayers(finalPageIndex, pageSize);

            getAutoUnwhitelistExemptPlayers.thenAcceptAsync(result -> {
                ArrayList<UUID> playerIds = result.getPlayerIds();
                int maxPages = result.getMaxPageCount();

                if (playerIds.isEmpty()) {
                    TextComponent noResultsToShow = Component.text()
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text("There are no results to be shown."))
                        .build();
                    
                    sender.sendMessage(noResultsToShow);
                    return;
                }
                
                TextComponent listPrompt = Component.text()
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("Auto Unwhitelist Exempt List"))
                    .build();

                sender.sendMessage(listPrompt);
                ExemptListChatHelpers exemptListChatHelpers = new ExemptListChatHelpers(this.plugin, this.mojangApi);

                Component baseMessage = exemptListChatHelpers.buildPaginationMessage(finalPageIndex, maxPages,
                        "/exe list", playerIds);
                sender.sendMessage(baseMessage);

                if (!(sender instanceof Player) && finalPageIndex != maxPages) {
                    TextComponent fetchNextPagePrompt = Component.text()
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text("To fetch the next page you need to use ["))
                        .color(NamedTextColor.GREEN)
                        .append(Component.text("/exe list " + (finalPageIndex + 1)))
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text("To fetch the next page you need to use ]"))
                        .build();

                    sender.sendMessage(fetchNextPagePrompt);
                }
            });
        } else if (command.equalsIgnoreCase("clear")) {
            CompletableFuture<Void> clearFuture = this.whitelistExemptDatabaseManager.clear();
            clearFuture.thenComposeAsync(i -> {
                TextComponent exemptListCleared = Component.text()
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("Auto unwhitelist exempt list cleared"))
                    .build();

                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(exemptListCleared));

                return new CompletableFuture<>();
            });
        } else {
            if (args.length < 2) {
                TextComponent mustSpecifyPlayer = Component.text()
                    .color(NamedTextColor.RED)
                    .append(Component.text("You must specify a player"))
                    .build();

                sender.sendMessage(mustSpecifyPlayer);
                return true;
            }

            if (!sender.isOp() && !sender.hasPermission("tws.exempt")) {
                TextComponent youDoNotHavePermissions = Component.text()
                    .color(NamedTextColor.RED)
                    .append(Component.text("You do not have permission to use this command"))
                    .build();

                sender.sendMessage(youDoNotHavePermissions);
                return true;
            }

            String target = args[1];
            CompletableFuture<UUID> playerIdFuture = this.mojangApi.getPlayerId(target);

            playerIdFuture
                    .thenComposeAsync(whitelistExemptDatabaseManager::isExempt)
                    .thenCombineAsync(playerIdFuture, (isExempt, uuid) -> new ExemptFutureModel(uuid, isExempt))
                    .thenComposeAsync(whitelistExemptFutureModel -> {
                        if (whitelistExemptFutureModel.getIsExempt()) {
                            // Exempt
                            if (command.equalsIgnoreCase("add")) {
                                TextComponent isAlreadyExempt = Component.text()
                                    .color(NamedTextColor.RED)
                                    .append(Component.text(target))
                                    .appendSpace()
                                    .append(Component.text("is already auto unwhitelist exempt"))
                                    .build();

                                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(isAlreadyExempt));
                                return new CompletableFuture<>();
                            }

                            TextComponent playerWasRemoved = Component.text()
                                .color(NamedTextColor.GREEN)
                                .append(Component.text(target))
                                .appendSpace()
                                .append(Component.text("was removed from the auto unwhitelist exempt list"))
                                .build();

                            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(playerWasRemoved));
                            return whitelistExemptDatabaseManager.remove(whitelistExemptFutureModel.getPlayerId());
                        } else {
                            // Not exempt
                            if (command.equalsIgnoreCase("add")) {
                                TextComponent playerWasAdded = Component.text()
                                    .color(NamedTextColor.GREEN)
                                    .append(Component.text(target))
                                    .appendSpace()
                                    .append(Component.text("was added to auto unwhitelist exempt list"))
                                    .build();

                                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(playerWasAdded));
                                return whitelistExemptDatabaseManager.add(whitelistExemptFutureModel.getPlayerId());
                            }

                            TextComponent playerIsNotExempt = Component.text()
                                .color(NamedTextColor.RED)
                                .append(Component.text(target))
                                .appendSpace()
                                .append(Component.text("is not auto unwhitelist exempt"))
                                .build();

                            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(playerIsNotExempt));
                            return new CompletableFuture<>();
                        }
                    });
        }

        return true;
    }
}

