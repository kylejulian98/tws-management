package dev.kylejulian.tws.commands;

import dev.kylejulian.tws.data.MojangApi;
import dev.kylejulian.tws.data.entities.EntityExemptList;
import dev.kylejulian.tws.data.interfaces.IExemptDatabaseManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
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
import java.util.concurrent.ExecutionException;

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
                ComponentBuilder baseMessage = buildPaginationMessage(finalPageIndex, maxPages, playerIds);
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

    private @NotNull ComponentBuilder buildPaginationMessage(int pageIndex, int maxPages, @NotNull ArrayList<UUID> playerIds) {
        int nextPage = pageIndex + 1;
        int prevPage = pageIndex - 1;

        TextComponent newLine = new TextComponent("\n");
        ComponentBuilder baseMessage = new ComponentBuilder();

        baseMessage.append(newLine);

        for (UUID id : playerIds) {
            CompletableFuture<Player> playerIsOnServerFuture = CompletableFuture.supplyAsync(() -> this.plugin.getServer().getPlayer(id));

            CompletableFuture<String> playerNameFuture = playerIsOnServerFuture.thenCompose(player -> {
                if (player == null) {
                    return this.mojangApi.getPlayerName(id);
                } else {
                    return CompletableFuture.supplyAsync(player::getDisplayName);
                }
            });

            CompletableFuture<String> validPlayerNameFuture = playerNameFuture.thenApply(s -> {
                if (s == null || s.equals("")) {
                    return id.toString();
                }
                return s;
            });

            String playerName = null;
            try {
                playerName = validPlayerNameFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            if (playerName != null) {
                baseMessage.append(ChatColor.YELLOW + "Player (" + ChatColor.GREEN + playerName + ChatColor.YELLOW + ")");
                baseMessage.append(newLine);
            }
        }

        baseMessage.append(newLine).append(ChatColor.RED + "<--" + ChatColor.RESET);

        if (prevPage > 0) { // Only allow user to go back if they can
            HoverEvent previousPageHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to go to the previous Page"));
            baseMessage.event(previousPageHoverEvent).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/exe list " + prevPage));
            baseMessage.append("").reset(); // Fix to prevent the rest of the baseMessage being associated to this event.
        }

        baseMessage.append(ChatColor.YELLOW + " Page (" + ChatColor.GREEN + pageIndex + "/" + maxPages + ChatColor.YELLOW + ") " + ChatColor.RESET)
                .append(ChatColor.RED + "-->" + ChatColor.RESET);

        if (nextPage <= maxPages) {// Only allow user to go forward if they can
            HoverEvent nextPageHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to go to the next Page"));
            baseMessage.event(nextPageHoverEvent).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/exe list " + nextPage));
        }

        baseMessage.append(newLine);

        return baseMessage;
    }

}

class WhitelistExemptFutureModel {

    private final UUID playerId;

    private final Boolean isExempt;

    public WhitelistExemptFutureModel(final UUID playerId, final Boolean isExempt) {
        this.playerId = playerId;
        this.isExempt = isExempt;
    }

    /**
     * Get the Player Id
     *
     * @return Player Id
     */
    public UUID getPlayerId() {
        return this.playerId;
    }

    /**
     * Get whether or not the Player was Exempt
     *
     * @return Is auto unwhitelist Exempt
     */
    public Boolean getIsExempt() {
        return this.isExempt;
    }
}
