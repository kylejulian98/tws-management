package dev.kylejulian.tws.commands;

import dev.kylejulian.tws.data.interfaces.IHudDatabaseManager;
import dev.kylejulian.tws.player.hud.events.HudEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HudCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final IHudDatabaseManager hudDatabaseManager;

    public HudCommand(@NotNull final JavaPlugin plugin, @NotNull final IHudDatabaseManager hudDatabaseManager) {
        this.plugin = plugin;
        this.hudDatabaseManager = hudDatabaseManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            final UUID playerId = player.getUniqueId();

            CompletableFuture<Boolean> playerHudIsEnabledFuture = hudDatabaseManager.isEnabled(playerId);
            playerHudIsEnabledFuture.thenAcceptAsync(result -> {
                if (!result) {
                    CompletableFuture<Void> enablePlayerHudFuture = hudDatabaseManager.addPlayer(playerId);
                    enablePlayerHudFuture.join();

                    raiseHudEvent(playerId, true);
                } else {
                    CompletableFuture<Void> disablePlayerHudFuture = hudDatabaseManager.removePlayer(playerId);
                    disablePlayerHudFuture.join();

                    raiseHudEvent(playerId, false);
                }
            });

            return true;
        }
        return false;
    }

    /**
     * Raises a Event to trigger Hud event service
     *
     * @param playerId Player whom triggered the event
     * @param enabled To indicate if a player wants the Hud to be visible or not
     */
    private void raiseHudEvent(@NotNull final UUID playerId, final boolean enabled) {
        Runnable runnable = () -> plugin.getServer().getPluginManager().callEvent(new HudEvent(playerId, enabled));
        this.plugin.getServer().getScheduler().runTask(this.plugin, runnable);
    }
}
