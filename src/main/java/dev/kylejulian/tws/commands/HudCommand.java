package dev.kylejulian.tws.commands;

import dev.kylejulian.tws.data.interfaces.IHudDatabaseManager;
import dev.kylejulian.tws.player.hud.events.HudEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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

            hudDatabaseManager.isEnabled(playerId, result -> {
                if (!result) {
                    hudDatabaseManager.addPlayer(playerId, null);
                    raiseHudEvent(playerId, true);
                } else {
                    hudDatabaseManager.removePlayer(playerId, null);
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
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getPluginManager().callEvent(new HudEvent(playerId, enabled));
            }
        }.runTask(this.plugin);
    }
}
