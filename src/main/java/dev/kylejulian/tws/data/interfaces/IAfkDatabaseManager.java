package dev.kylejulian.tws.data.interfaces;

import dev.kylejulian.tws.data.callbacks.AfkKickExemptListQueryCallback;
import dev.kylejulian.tws.data.callbacks.BooleanQueryCallback;

import java.util.UUID;

public interface IAfkDatabaseManager {

    void addPlayerToKickExempt(final UUID playerId, final BooleanQueryCallback callback);

    void removePlayerFromKickExempt(final UUID playerId, final BooleanQueryCallback callback);

    void isPlayerKickExempt(final UUID playerId, final BooleanQueryCallback callback);

    void getAfkKickExemptPlayers(final int pageIndex, final int pageSize, final AfkKickExemptListQueryCallback callback);
}
