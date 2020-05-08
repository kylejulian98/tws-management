package dev.kylejulian.tws.data.interfaces;

import dev.kylejulian.tws.data.callbacks.BooleanQueryCallback;

public interface IDatabaseManager {

    void setupDefaultSchema(final BooleanQueryCallback callback);
}
