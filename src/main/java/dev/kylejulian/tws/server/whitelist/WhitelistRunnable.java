package dev.kylejulian.tws.server.whitelist;

import dev.kylejulian.tws.configuration.WhitelistConfigModel;
import dev.kylejulian.tws.server.LuckPermsHelper;
import net.luckperms.api.LuckPerms;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WhitelistRunnable implements Runnable {

    private final JavaPlugin plugin;
    private final WhitelistConfigModel whitelistConfigModel;

    public WhitelistRunnable(@NotNull JavaPlugin plugin, @NotNull WhitelistConfigModel whitelistConfigModel) {
        this.plugin = plugin;
        this.whitelistConfigModel = whitelistConfigModel;
    }

    @Override
    public void run() {
        if (!this.whitelistConfigModel.getEnabled()) {
            this.plugin.getLogger().log(Level.INFO, "Whitelist check has not been performed as it is disabled");
            return;
        }

        int noOfDays = this.whitelistConfigModel.getDays();
        int noOfHours = this.whitelistConfigModel.getHours();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime unwhitelistDate = now.minusDays(noOfDays).minusHours(noOfHours);

        LuckPermsHelper luckPermsHelper = getLuckPermsHelper();
        Set<OfflinePlayer> whitelistedPlayers = this.plugin.getServer().getWhitelistedPlayers();
        Set<CompletableFuture<Void>> completableFutures = new HashSet<>();

        for (OfflinePlayer whitelistedPlayer : whitelistedPlayers) {
            UUID playerId = whitelistedPlayer.getUniqueId();

            CompletableFuture<Boolean> playerHasPermissionFuture;

            if (luckPermsHelper != null) {
                playerHasPermissionFuture = luckPermsHelper.hasPermissionAsync(playerId, "tws.exempt.auto");
            } else { // Luck Perms provider was null i.e. not enabled
                playerHasPermissionFuture = CompletableFuture.supplyAsync(() -> false); // We return false to indicate the player is not exempt
            }

            CompletableFuture<PlayerWhitelisted> playerToBeUnwhitelistedFuture = getVerifyWhitelistedPlayerFuture(unwhitelistDate, whitelistedPlayer, playerHasPermissionFuture);
            CompletableFuture<Void> unwhitelistPlayerFuture = getPlayerNeedsToUnwhitelistedFuture(playerToBeUnwhitelistedFuture);

            completableFutures.add(unwhitelistPlayerFuture);
        }

        this.plugin.getLogger().log(Level.INFO, "Verifying {0} Players for auto Unwhitelist!", completableFutures.size());
        completableFutures.forEach(CompletableFuture::join);
    }

    private @Nullable LuckPermsHelper getLuckPermsHelper() {
        LuckPermsHelper luckPermsHelper = null;
        try {
            RegisteredServiceProvider<LuckPerms> provider =
                    this.plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);

            if (provider != null) { // Luck Perms is enabled
                luckPermsHelper = new LuckPermsHelper(this.plugin, provider.getProvider());
            }
        } catch (NoClassDefFoundError e) {
            this.plugin.getLogger().log(Level.WARNING, "LuckPerms is not enabled, skipping permission node check for auto unwhitelist!");
        }
        return luckPermsHelper;
    }

    private @NotNull CompletableFuture<Void> getPlayerNeedsToUnwhitelistedFuture(@NotNull CompletableFuture<PlayerWhitelisted> future) {
        return future.thenAcceptAsync(player -> {
            if (!player.getWhitelist()) { // Player needs to be unwhitelisted
                Runnable unwhitelistPlayerTask = () -> {
                    UUID offlinePlayerId = player.getPlayerId();
                    OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(offlinePlayerId);
                    offlinePlayer.setWhitelisted(false);
                };
                this.plugin.getServer().getScheduler().runTask(this.plugin, unwhitelistPlayerTask);
            }
        });
    }

    private @NotNull CompletableFuture<PlayerWhitelisted> getVerifyWhitelistedPlayerFuture(@NotNull LocalDateTime unwhitelistDate, @NotNull OfflinePlayer whitelistedPlayer,
                                                                                           @NotNull CompletableFuture<Boolean> future) {
        return future.thenApplyAsync(result -> {
            PlayerWhitelisted playerWhitelisted = new PlayerWhitelisted(whitelistedPlayer.getUniqueId(), true);

            if (result) { // Player has Permission
                return playerWhitelisted;
            } else {
                long lastPlayTime = whitelistedPlayer.getLastPlayed();

                // If Player is exempt in database

                if (lastPlayTime == 0L) { // Player has not joined
                    return playerWhitelisted;
                }

                LocalDateTime lastPlayDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastPlayTime), ZoneId.systemDefault());
                long days = lastPlayDate.until(LocalDateTime.now(), ChronoUnit.DAYS);
                if (lastPlayDate.isBefore(unwhitelistDate)) {
                    this.plugin.getLogger().log(Level.INFO, "Player ({0}) has been unwhitelisted due to inactivity! Has been inactive for : {1} days!",
                            new Object[] { whitelistedPlayer.getName(), days });

                    playerWhitelisted.setWhitelist(false);
                    return playerWhitelisted;
                }

                this.plugin.getLogger().log(Level.FINE, "Player ({0}) will not be unwhitelisted! Has been inactive for : {1} days!",
                        new Object[] { whitelistedPlayer.getName(), days });
            }

            return playerWhitelisted;
        });
    }
}

