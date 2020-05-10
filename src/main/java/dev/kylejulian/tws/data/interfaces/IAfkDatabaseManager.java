package dev.kylejulian.tws.data.interfaces;

import dev.kylejulian.tws.data.callbacks.AfkKickExemptListQueryCallback;
import dev.kylejulian.tws.data.callbacks.BooleanQueryCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IAfkDatabaseManager extends IDatabaseManager {

    void addPlayer(@NotNull final UUID playerId, @Nullable final BooleanQueryCallback callback);

    void removePlayer(@NotNull final UUID playerId, @Nullable final BooleanQueryCallback callback);

    void isKickExempt(@NotNull final UUID playerId, @Nullable final BooleanQueryCallback callback);

    void getPlayers(final int pageIndex, final int pageSize, @NotNull final AfkKickExemptListQueryCallback callback);
}
