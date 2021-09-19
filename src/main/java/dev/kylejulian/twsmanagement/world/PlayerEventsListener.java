package dev.kylejulian.twsmanagement.world;

import dev.kylejulian.twsmanagement.afk.events.AfkCancelledEvent;
import dev.kylejulian.twsmanagement.afk.events.AfkEvent;
import dev.kylejulian.twsmanagement.configuration.NightResetConfigModel;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;

public class PlayerEventsListener implements Listener {

    private final ArrayList<UUID> afkPlayers;
    private final ArrayList<UUID> sleepingPlayers;
    private final JavaPlugin plugin;
    private final NightResetConfigModel nightResetConfigModel;

    private final HashMap<UUID, Integer> accelerateNightTasks;

    public PlayerEventsListener(@NotNull JavaPlugin plugin, @NotNull NightResetConfigModel nightResetConfigModel) {
        this.afkPlayers = new ArrayList<>();
        this.sleepingPlayers = new ArrayList<>();
        this.plugin = plugin;
        this.nightResetConfigModel = nightResetConfigModel;
        this.accelerateNightTasks = new HashMap<>();
    }

    @EventHandler
    public void onAfk(final AfkEvent e) {
        UUID playerId = e.getPlayerId();
        if (!this.afkPlayers.contains(playerId)){
            this.afkPlayers.add(playerId);
        }
    }

    @EventHandler
    public void onAfkCancelled(final AfkCancelledEvent e) {
        UUID playerId = e.getPlayerId();
        this.afkPlayers.remove(playerId);
    }

    @EventHandler
    public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
        if (!e.isCancelled() && e.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
            Player player = e.getPlayer();
            UUID playerId = player.getUniqueId();
            this.sleepingPlayers.add(playerId);

            boolean nightResetRulesMet = verifyNightResetRules();
            if (nightResetRulesMet) {
                setNormalWorldsToDay();
            }
        }
    }

    @EventHandler
    public void onPlayerBedExit(final PlayerBedLeaveEvent e) {
        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();
        this.sleepingPlayers.remove(playerId);

        tryStopNightSkipTasks(player.getWorld().getUID()); // We could have many worlds that are being set to day
    }

    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent e) {
        boolean nightResetRulesMet = verifyNightResetRules();
        if (nightResetRulesMet) {
            setNormalWorldsToDay();
        }

        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();
        this.afkPlayers.remove(playerId); // Clean-up if Player is AFK and they disconnect
        this.sleepingPlayers.remove(playerId); // Clean-up if Player is in Bed and they disconnect
    }

    /**
     * Handles setting the world(s) to day
     */
    private void setNormalWorldsToDay() {
        Collection<World> worlds = this.plugin.getServer().getWorlds();

        for (World world : worlds) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                UUID worldId = world.getUID();
                tryStopNightSkipTasks(worldId);

                AccelerateNightTimeRunnable accelerateNightTimeRunnable =
                        new AccelerateNightTimeRunnable(this.plugin, worldId);
                BukkitTask accelerateNightTask =
                        accelerateNightTimeRunnable.runTaskTimer(this.plugin, 0, 1);
                this.accelerateNightTasks.put(worldId, accelerateNightTask.getTaskId());
            }
        }
        this.plugin.getLogger().log(Level.INFO, "Tasks created to set night to day");
    }

    /**
     * Attempts to cancel the AccelerateNightTime task for the given world
     * @param playerWorldId World Id of Player who triggered the cancellation
     */
    private void tryStopNightSkipTasks(UUID playerWorldId) {
        if (this.accelerateNightTasks.containsKey(playerWorldId)) {
            Integer taskId = this.accelerateNightTasks.get(playerWorldId);
            this.plugin.getServer().getScheduler().cancelTask(taskId);
            this.accelerateNightTasks.remove(playerWorldId);
        }
    }

    /**
     * Checks all the validation rules for setting night to day have been met
     */
    private boolean verifyNightResetRules() {
        if (!this.nightResetConfigModel.getEnabled()) { // Not enabled, skip
            return false;
        }

        Collection<? extends Player> onlinePlayers = this.plugin.getServer().getOnlinePlayers();
        for (Player player : onlinePlayers) {
            UUID playerId = player.getUniqueId();

            if (this.sleepingPlayers.contains(playerId)) { // Player is asleep
                continue;
            }

            if (this.afkPlayers.contains(playerId)) { // Player is AFK
                continue;
            }

            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) { // Player is other World
                continue;
            }

            if (isDay(player.getWorld())) {
                return false;
            }

            return false; // A Player has not met all the rules, finish early
        }

        return this.sleepingPlayers.size() > 0; // At least one Player has to be asleep for this reset to work
    }

    /**
     * Checks if World is currently in Daytime
     * @param world World to examine
     * @return If the world is in Daytime or not
     */
    private boolean isDay(World world) {
        return world.getTime() < 12300 || world.getTime() > 23850;
    }
}
