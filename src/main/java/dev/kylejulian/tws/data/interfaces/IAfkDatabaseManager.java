package dev.kylejulian.tws.data.interfaces;

import dev.kylejulian.tws.data.entities.AfkKickExemptList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IAfkDatabaseManager extends IDatabaseManager {

    @NotNull CompletableFuture<Void> addPlayer(@NotNull final UUID playerId);

    @NotNull CompletableFuture<Void> removePlayer(@NotNull final UUID playerId);

    @NotNull CompletableFuture<Boolean> isKickExempt(@NotNull final UUID playerId);

    @NotNull CompletableFuture<AfkKickExemptList> getPlayers(final int pageIndex, final int pageSize);
}
