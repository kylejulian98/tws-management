package dev.kylejulian.twsmanagement.server.whitelist;

import dev.kylejulian.twsmanagement.configuration.WhitelistConfigModel;
import dev.kylejulian.twsmanagement.data.interfaces.IExemptDatabaseManager;
import dev.kylejulian.twsmanagement.server.LuckPermsHelper;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public record WhitelistRunnable(JavaPlugin plugin,
                                WhitelistConfigModel whitelistConfigModel,
                                IExemptDatabaseManager whitelistExemptDatabaseManager) implements Runnable {

    public WhitelistRunnable(@NotNull JavaPlugin plugin, @NotNull WhitelistConfigModel whitelistConfigModel,
                             @NotNull IExemptDatabaseManager whitelistExemptDatabaseManager) {
        this.plugin = plugin;
        this.whitelistConfigModel = whitelistConfigModel;
        this.whitelistExemptDatabaseManager = whitelistExemptDatabaseManager;
    }

    @Override
    public void run() {
        if (!this.whitelistConfigModel.getEnabled()) {
            this.plugin.getLogger().log(Level.INFO, "Whitelist check has not been performed as it is disabled");
            return;
        }

        Duration inactivity = this.whitelistConfigModel.getInactivity();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime unwhitelistDate = now.minus(inactivity);

        LuckPermsHelper luckPermsHelper = getLuckPermsHelper();
        Set<OfflinePlayer> whitelistedPlayers = this.plugin.getServer().getWhitelistedPlayers();
        CompletableFuture<?>[] completableFutures = new CompletableFuture<?>[whitelistedPlayers.size()];

        OfflinePlayer[] whitelistedPlayersArray = whitelistedPlayers.toArray(new OfflinePlayer[0]);
        for (int i = 0; i < whitelistedPlayers.size(); i++) {
            UUID playerId = whitelistedPlayersArray[i].getUniqueId();

            CompletableFuture<Boolean> playerHasPermissionFuture;

            if (luckPermsHelper != null) {
                playerHasPermissionFuture = luckPermsHelper.hasPermissionAsync(playerId, "tws.exempt.auto");
            } else { // Luck Perms provider was null i.e. not enabled
                // We return false to indicate the player is not exempt
                playerHasPermissionFuture = CompletableFuture.supplyAsync(() -> false);
            }

            CompletableFuture<Boolean> playerIsExempt =
                    playerHasPermissionFuture.thenCombineAsync(whitelistExemptDatabaseManager.isExempt(playerId),
                            (playerHasPermission, playerDatabaseExempt) -> {
                                if (playerHasPermission) {
                                    return true;
                                }

                                return playerDatabaseExempt;
                            });

            CompletableFuture<PlayerWhitelisted> playerToBeUnwhitelistedFuture =
                    getVerifyWhitelistedPlayerFuture(unwhitelistDate, whitelistedPlayersArray[i], playerIsExempt);
            CompletableFuture<Void> unwhitelistPlayerFuture =
                    getPlayerNeedsToUnwhitelistedFuture(playerToBeUnwhitelistedFuture);
            completableFutures[i] = unwhitelistPlayerFuture;
        }

        this.plugin.getLogger().log(Level.INFO, "Verifying {0} Players for auto Unwhitelist!",
                completableFutures.length);

        CompletableFuture.allOf(completableFutures);
    }

    private @Nullable
    LuckPermsHelper getLuckPermsHelper() {
        LuckPermsHelper luckPermsHelper = null;
        try {
            // Fully qualified names as to avoid issues creating the class when LP isn't deployed with this plugin
            RegisteredServiceProvider<net.luckperms.api.LuckPerms> provider =
                    this.plugin.getServer().getServicesManager().getRegistration(net.luckperms.api.LuckPerms.class);

            if (provider != null) { // Luck Perms is enabled
                luckPermsHelper = new LuckPermsHelper(this.plugin, provider.getProvider());
            }
        } catch (NoClassDefFoundError e) {
            this.plugin.getLogger().log(Level.WARNING,
                    "LuckPerms is not enabled, skipping permission node check for auto unwhitelist!");
        }
        return luckPermsHelper;
    }

    private @NotNull
    CompletableFuture<Void> getPlayerNeedsToUnwhitelistedFuture(
            @NotNull CompletableFuture<PlayerWhitelisted> future) {
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

    private @NotNull
    CompletableFuture<PlayerWhitelisted>
    getVerifyWhitelistedPlayerFuture(@NotNull LocalDateTime unwhitelistDate,
                                     @NotNull OfflinePlayer whitelistedPlayer,
                                     @NotNull CompletableFuture<Boolean> future) {

        return future.thenApplyAsync(result -> {
            PlayerWhitelisted playerWhitelisted =
                    new PlayerWhitelisted(whitelistedPlayer.getUniqueId(), true);

            if (result) { // Player has Permission
                return playerWhitelisted;
            } else {
                long lastPlayTime = whitelistedPlayer.getLastLogin();

                // If Player is exempt in database

                if (lastPlayTime == 0L) { // Player has not joined
                    return playerWhitelisted;
                }

                LocalDateTime lastPlayDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastPlayTime),
                        ZoneId.systemDefault());
                long days = lastPlayDate.until(LocalDateTime.now(), ChronoUnit.DAYS);
                if (lastPlayDate.isBefore(unwhitelistDate)) {
                    this.plugin.getLogger().log(Level.INFO,
                            "Player ({0}) has been unwhitelisted due to inactivity! " +
                                    "Has been inactive for : {1} days!",
                            new Object[]{whitelistedPlayer.getName(), days});

                    playerWhitelisted.setWhitelist(false);
                    return playerWhitelisted;
                }

                this.plugin.getLogger().log(Level.FINE,
                        "Player ({0}) will not be unwhitelisted! Has been inactive for : {1} days!",
                        new Object[]{whitelistedPlayer.getName(), days});
            }

            return playerWhitelisted;
        });
    }
}

