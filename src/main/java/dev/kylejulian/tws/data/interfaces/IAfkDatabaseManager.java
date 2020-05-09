package dev.kylejulian.tws.data.interfaces;

import dev.kylejulian.tws.data.callbacks.AfkKickExemptListQueryCallback;
import dev.kylejulian.tws.data.callbacks.BooleanQueryCallback;

import java.util.UUID;

public interface IAfkDatabaseManager extends IDatabaseManager {

    void addPlayer(final UUID playerId, final BooleanQueryCallback callback);

    void removePlayer(final UUID playerId, final BooleanQueryCallback callback);

    void isKickExempt(final UUID playerId, final BooleanQueryCallback callback);

    void getPlayers(final int pageIndex, final int pageSize, final AfkKickExemptListQueryCallback callback);
}
