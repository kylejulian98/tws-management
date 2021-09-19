package dev.kylejulian.twsmanagement.player.hud;

import dev.kylejulian.twsmanagement.configuration.HudConfigModel;
import dev.kylejulian.twsmanagement.player.hud.events.HudEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class HudListener implements Listener {

    private final JavaPlugin plugin;
    private final HashMap<UUID, Integer> playerTask;
    private final HudConfigModel hudConfig;

    public HudListener(@NotNull JavaPlugin plugin, @NotNull HudConfigModel hudConfig) {
        this.plugin = plugin;
        this.playerTask = new HashMap<>();
        this.hudConfig = hudConfig;
    }

    @EventHandler
    public void onLeave(@NotNull PlayerQuitEvent e) {
        final UUID playerId = e.getPlayer().getUniqueId();
        if (this.playerTask.containsKey(playerId)) {
            Integer taskId = this.playerTask.get(playerId);
            this.plugin.getServer().getScheduler().cancelTask(taskId);
            this.playerTask.remove(playerId);
        }
    }

    @EventHandler
    public void onHudRaised(@NotNull final HudEvent e) {
        if (!this.hudConfig.getEnabled()) {
            return;
        }

        final UUID playerId = e.getPlayerId();
        if (e.getEnabled()) { // Player wants task enabled
            if (!this.playerTask.containsKey(playerId)) {
                Integer refreshRateTicks = this.hudConfig.getRefreshRateTicks();
                if (refreshRateTicks < 1) {
                    this.plugin.getLogger().log(Level.WARNING,
                            "Invalid configuration for Hud Refresh Rate. Setting Refresh Rate to 10 ticks.");
                    refreshRateTicks = 10;
                }

                final BukkitTask runnable = new HudDisplayRunnable(this.plugin, playerId)
                        .runTaskTimerAsynchronously(this.plugin, 0, refreshRateTicks); // 10 ticks = 0.5 seconds

                final Integer taskId = runnable.getTaskId();
                this.playerTask.put(playerId, taskId);
            }
        }
        else {
            if (this.playerTask.containsKey(playerId)) {
                Integer taskId = this.playerTask.get(playerId);
                this.plugin.getServer().getScheduler().cancelTask(taskId);

                this.playerTask.remove(playerId);
            }
        }
    }
}
