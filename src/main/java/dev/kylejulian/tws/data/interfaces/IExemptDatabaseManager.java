package dev.kylejulian.tws.data.interfaces;

import dev.kylejulian.tws.data.entities.EntityExemptList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IExemptDatabaseManager extends IDatabaseManager {

    /**
     * Checks if a Player is exempt from the auto unwhitelist
     * @param playerId Player to check
     * @return Whether or not the Player is exempt
     */
    @NotNull CompletableFuture<Boolean> isExempt(@NotNull UUID playerId);

    /**
     * Gets all the Players in the Whitelist exempt table, based on the pageIndex and pageSize variables
     * @param pageIndex page index e.g. how many to skip
     * @param pageSize page size e.g. how many to retrieve
     * @return A object containing the Players in the database and the pageIndex and pageSize variables
     */
    @NotNull CompletableFuture<EntityExemptList> getPlayers(final int pageIndex, final int pageSize);

    /**
     * Add Player to the Exempt table
     * @param playerId Player to add
     * @return A future in which a specified Player has been made exempt
     */
    @NotNull CompletableFuture<Void> add(@NotNull UUID playerId);

    /**
     * Remove a Player from the Exempt table
     * @param playerId Player to remove
     * @return A future in which a specified Player is no longer exempt
     */
    @NotNull CompletableFuture<Void> remove(@NotNull UUID playerId);
}
