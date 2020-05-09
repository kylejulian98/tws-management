package dev.kylejulian.tws.player.hud;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class HudDisplayRunnable extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final UUID playerId;

    private final String xDisplay = "\u00a7a\u00a7lX \u00a7r%s ";
    private final String yDisplay = "\u00a7a\u00a7lY \u00a7r%s ";
    private final String zDisplay = "\u00a7a\u00a7lZ \u00a7r%s ";
    private final String orientationDisplay = "\u00a7e\u00a7l%s ";
    private final String timeDisplay = "\u00a7c\u00a7l%s";

    public HudDisplayRunnable(@NotNull JavaPlugin plugin, @NotNull UUID playerId) {
        this.playerId = playerId;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        final Player player = this.plugin.getServer().getPlayer(this.playerId);
        if (player == null) {
            return;
        }

        final Location location = player.getLocation();
        final Integer x = location.getBlockX();
        final Integer y = location.getBlockY();
        final Integer z = location.getBlockZ();

        final long playerWorldTime = player.getWorld().getTime();
        final float playerYaw = location.getYaw();

        final String xFormatted = String.format(this.xDisplay, x);
        final String yFormatted = String.format(this.yDisplay, y);
        final String zFormatted = String.format(this.zDisplay, z);
        final String orientationFormatted = String.format(this.orientationDisplay, Orientation.getOrientation(playerYaw));
        final String timeFormatted = String.format(this.timeDisplay, Time.ticksToTime(playerWorldTime));

        BaseComponent baseComponent = new TextComponent(xFormatted);
        baseComponent.addExtra(new TextComponent(yFormatted));
        baseComponent.addExtra(new TextComponent(zFormatted));
        baseComponent.addExtra(new TextComponent(orientationFormatted));
        baseComponent.addExtra(new TextComponent(timeFormatted));

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, baseComponent);
    }
}
