package dev.kylejulian.tws.data.interfaces;

import dev.kylejulian.tws.data.callbacks.BooleanQueryCallback;

import java.util.UUID;

public interface IHudDatabaseManager extends IDatabaseManager {

    void isEnabled(final UUID playerId, final BooleanQueryCallback callback);

    void removePlayer(final UUID playerId, final BooleanQueryCallback callback);

    void addPlayer(final UUID playerId, final BooleanQueryCallback callback);
}
