package dev.kylejulian.twsmanagement.commands;

import dev.kylejulian.twsmanagement.afk.events.AfkCommandEvent;
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
import java.util.logging.Level;

public record AfkCommand(JavaPlugin plugin,
                         IExemptDatabaseManager afkDatabaseManager,
                         MojangApi mojangApi) implements CommandExecutor {

    public AfkCommand(@NotNull JavaPlugin plugin, @NotNull IExemptDatabaseManager afkDatabaseManager,
                      @NotNull MojangApi mojangApi) {
        this.plugin = plugin;
        this.afkDatabaseManager = afkDatabaseManager;
        this.mojangApi = mojangApi;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
                             String[] args) {
        if (args.length < 1) {
            if (sender instanceof Player player) {
                AfkCommandEvent event = new AfkCommandEvent(player.getUniqueId());
                Runnable afkEventTask = () -> this.plugin.getServer().getPluginManager().callEvent(event);

                this.plugin.getServer().getScheduler().runTask(this.plugin, afkEventTask);
            } else {
                this.plugin.getServer().getLogger().log(Level.WARNING, "You must be a Player to use this command!");
            }
        } else if (args.length > 1) {
            String base = args[0];
            String command = args[1];

            if (!base.equalsIgnoreCase("exempt")) {
                TextComponent commandIsNotRecognised = Component.text()
                    .color(NamedTextColor.RED)
                    .append(Component.text("Command is not recognised"))
                    .build();

                sender.sendMessage(commandIsNotRecognised);
                return false;
            }

            if (!sender.hasPermission("tws.afk.exempt") && !sender.isOp()) {
                TextComponent noPermissions = Component.text()
                    .color(NamedTextColor.RED)
                    .append(Component.text("You do not have permissions to use this command."))
                    .build();

                sender.sendMessage(noPermissions);
                return true;
            }

            if (command.equalsIgnoreCase("list")) {
                return executeListSubcommand(sender, args);
            }

            if (command.equalsIgnoreCase("clear")) {
                return executeClearSubcommand(sender);
            }

            if (args.length < 3) {
                sender.sendMessage(NamedTextColor.RED + "You need to specify the correct number of arguments.");
                return false;
            }

            executeAddOrRemoveSubcommand(sender, args[2], command);
        }

        return true;
    }

    private void executeAddOrRemoveSubcommand(@NotNull CommandSender sender, String target, String command) {
        CompletableFuture<UUID> playerIdFuture = this.mojangApi.getPlayerId(target);

        playerIdFuture
                .thenComposeAsync(afkDatabaseManager::isExempt)
                .thenCombineAsync(playerIdFuture, (isExempt, uuid) -> new ExemptFutureModel(uuid, isExempt))
                .thenComposeAsync(whitelistExemptFutureModel -> {
                    if (whitelistExemptFutureModel.getIsExempt()) {
                        // Exempt
                        if (command.equalsIgnoreCase("add")) {
                            TextComponent targetAlreadyExempt = Component.text()
                                .color(NamedTextColor.RED)
                                .append(Component.text(target))
                                .appendSpace()
                                .append(Component.text("is already AFK Kick exempt"))
                                .build();

                            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(targetAlreadyExempt));
                            return new CompletableFuture<>();
                        }

                        TextComponent targetRemoved = Component.text()
                            .color(NamedTextColor.GREEN)
                            .append(Component.text(target))
                            .appendSpace()
                            .append(Component.text("was removed from the AFK Kick exempt list"))
                            .build();

                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(targetRemoved));

                        return afkDatabaseManager.remove(whitelistExemptFutureModel.getPlayerId());
                    } else {
                        // Not exempt
                        if (command.equalsIgnoreCase("add")) {
                            TextComponent targetAdded = Component.text()
                                .color(NamedTextColor.GREEN)
                                .append(Component.text(target))
                                .appendSpace()
                                .append(Component.text("was added to AFK Kick exempt list"))
                                .build();

                            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(targetAdded));
                            return afkDatabaseManager.add(whitelistExemptFutureModel.getPlayerId());
                        }
                        
                        TextComponent targetIsNotAfkList = Component.text()
                            .color(NamedTextColor.RED)
                            .append(Component.text(target))
                            .appendSpace()
                            .append(Component.text("is not AFK Kick exempt"))
                            .build();

                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(targetIsNotAfkList));
                        return new CompletableFuture<>();
                    }
                });
    }

    private boolean executeClearSubcommand(@NotNull CommandSender sender) {
        CompletableFuture<Void> clearFuture = this.afkDatabaseManager.clear();
        clearFuture.thenComposeAsync(i -> {
            TextComponent afkListCleared = Component.text()
                .color(NamedTextColor.YELLOW)
                .append(Component.text("AFK Kick Exempt List cleared"))
                .build();

            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(afkListCleared));

            return new CompletableFuture<>();
        });
        return true;
    }

    private boolean executeListSubcommand(@NotNull CommandSender sender, String[] args) {
        int pageIndex;

        if (args.length == 3) {
            try {
                pageIndex = Integer.parseInt(args[2]);
                if (pageIndex < 1) {
                    pageIndex = 1;
                }
            } catch (NumberFormatException e) {
                TextComponent invalidPageNumber = Component.text()
                    .color(NamedTextColor.RED)
                    .append(Component.text("You must specify a valid page number!"))
                    .build();

                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(invalidPageNumber));
                return false;
            }
        } else {
            pageIndex = 1;
        }

        final int finalPageIndex = pageIndex;
        int pageSize = 5;
        CompletableFuture<EntityExemptList> getAfkExemptPlayers =
                this.afkDatabaseManager.getPlayers(pageIndex, pageSize);

        getAfkExemptPlayers
                .thenAcceptAsync(result -> {
                    ArrayList<UUID> playerIds = result.getPlayerIds();
                    int maxPages = result.getMaxPageCount();

                    if (playerIds.isEmpty()) {
                        TextComponent afkExemptListEmpty = Component.text()
                            .color(NamedTextColor.YELLOW)
                            .append(Component.text("There are no results to be shown."))
                            .build();

                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(afkExemptListEmpty));
                        return;
                    }

                    TextComponent afkListPrompt = Component.text()
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text("AFK Kick Exempt List"))
                        .build();

                    this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(afkListPrompt));

                    ExemptListChatHelpers exemptListChatHelpers = new ExemptListChatHelpers(this.plugin, this.mojangApi);
                    Component baseMessage = exemptListChatHelpers.buildPaginationMessage(finalPageIndex,
                            maxPages, "/afk exempt list", playerIds);

                    this.plugin.getServer().getScheduler().runTask(this.plugin,
                            () -> sender.sendMessage(baseMessage));

                    if (!(sender instanceof Player) && finalPageIndex != maxPages) {
                        TextComponent afkText = Component.text()
                            .color(NamedTextColor.YELLOW)
                            .append(Component.text("To fetch the next page you need to use ["))
                            .color(NamedTextColor.GREEN)
                            .append(Component.text("/afk exempt list " + (finalPageIndex + 1)))
                            .color(NamedTextColor.YELLOW)
                            .append(Component.text("]"))
                            .build();

                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> sender.sendMessage(afkText));
                    }
                });

        return true;
    }
}
