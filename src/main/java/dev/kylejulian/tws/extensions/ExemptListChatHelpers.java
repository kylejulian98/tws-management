package dev.kylejulian.tws.extensions;

import dev.kylejulian.tws.data.MojangApi;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ExemptListChatHelpers {

    private final JavaPlugin plugin;
    private final MojangApi mojangApi;

    public ExemptListChatHelpers(final JavaPlugin plugin, final MojangApi mojangApi) {
        this.plugin = plugin;
        this.mojangApi = mojangApi;
    }

    public @NotNull ComponentBuilder buildPaginationMessage(int pageIndex, int maxPages, @NotNull String commandToNextPage, @NotNull ArrayList<UUID> playerIds) {
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
            baseMessage.event(previousPageHoverEvent).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandToNextPage + " " + prevPage));
            baseMessage.append("").reset(); // Fix to prevent the rest of the baseMessage being associated to this event.
        }

        baseMessage.append(ChatColor.YELLOW + " Page (" + ChatColor.GREEN + pageIndex + "/" + maxPages + ChatColor.YELLOW + ") " + ChatColor.RESET)
                .append(ChatColor.RED + "-->" + ChatColor.RESET);

        if (nextPage <= maxPages) {// Only allow user to go forward if they can
            HoverEvent nextPageHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to go to the next Page"));
            baseMessage.event(nextPageHoverEvent).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,  commandToNextPage + " " + nextPage));
        }

        baseMessage.append(newLine);

        return baseMessage;
    }
}
