package dev.kylejulian.tws.world;

import dev.kylejulian.tws.afk.events.AfkCancelledEvent;
import dev.kylejulian.tws.afk.events.AfkEvent;
import dev.kylejulian.tws.configuration.NightResetConfig;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

public class DaytimeListener implements Listener {

    private final ArrayList<UUID> afkPlayers;
    private final JavaPlugin plugin;
    private final NightResetConfig nightResetConfig;

    public DaytimeListener(JavaPlugin plugin, NightResetConfig nightResetConfig) {
        this.afkPlayers = new ArrayList<>();
        this.plugin = plugin;
        this.nightResetConfig = nightResetConfig;
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
        if (this.afkPlayers.contains(playerId)){
            this.afkPlayers.remove(playerId);
        }
    }

    @EventHandler
    public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
        if (!e.isCancelled()) {
            boolean nightResetRulesMet = verifyNightResetRules();
            if (nightResetRulesMet) {
                setNormalWorldsToDay();
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(final PlayerQuitEvent e) {
        boolean nightResetRulesMet = verifyNightResetRules();
        if (nightResetRulesMet) {
            setNormalWorldsToDay();
        }
    }

    private void setNormalWorldsToDay() {
        Collection<World> worlds = this.plugin.getServer().getWorlds();
        for (World world : worlds) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                world.setTime(1000);
            }
        }
        this.plugin.getLogger().log(Level.INFO, "Worlds have been set to daytime.");
    }

    private boolean verifyNightResetRules() {
        if (!this.nightResetConfig.getEnabled()) { // Not enabled, skip
            return false;
        }

        Collection<? extends Player> onlinePlayers = this.plugin.getServer().getOnlinePlayers();
        int sleepingCount = 0;
        for (Player player : onlinePlayers) {
            UUID playerId = player.getUniqueId();

            if (player.isSleeping()) { // Player is sleeping
                sleepingCount++;
                continue;
            }

            if (this.afkPlayers.contains(playerId)) { // Player is Afk
                continue;
            }

            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) { // Player is other World
                continue;
            }

            return false; // A Player has not met all the rules, finish early
        }

        return sleepingCount > 0; // At least one Player has to be asleep for this reset to work
    }
}
