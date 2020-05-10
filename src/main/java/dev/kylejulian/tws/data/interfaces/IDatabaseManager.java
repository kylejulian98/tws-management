package dev.kylejulian.tws.data.interfaces;

import dev.kylejulian.tws.data.callbacks.BooleanQueryCallback;
import org.jetbrains.annotations.Nullable;

public interface IDatabaseManager {

    void setupDefaultSchema(final @Nullable BooleanQueryCallback callback);
}
