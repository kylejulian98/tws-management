package dev.kylejulian.tws.data.interfaces;

import dev.kylejulian.tws.data.callbacks.BooleanQueryCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IHudDatabaseManager extends IDatabaseManager {

    void isEnabled(@NotNull final UUID playerId, @Nullable final BooleanQueryCallback callback);

    void removePlayer(@NotNull final UUID playerId, @Nullable final BooleanQueryCallback callback);

    void addPlayer(@NotNull final UUID playerId, @Nullable final BooleanQueryCallback callback);
}
