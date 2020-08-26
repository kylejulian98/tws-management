package dev.kylejulian.tws.world;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class AccelerateNightTimeRunnable extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final UUID worldId;

    public AccelerateNightTimeRunnable(JavaPlugin plugin, UUID worldId) {
        this.plugin = plugin;
        this.worldId = worldId;
    }

    @Override
    public void run() {
        World world = plugin.getServer().getWorld(worldId);

        if (world != null && !isDay(world)) {
            long worldTime = world.getTime();
            world.setTime(worldTime + 100);

            return;
        }

        this.cancel();
    }

    private boolean isDay(World world) {
        return world.getTime() < 12300 || world.getTime() > 23850;
    }
}
