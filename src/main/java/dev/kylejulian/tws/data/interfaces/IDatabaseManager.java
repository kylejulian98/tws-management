package dev.kylejulian.tws.data.interfaces;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface IDatabaseManager {

    @NotNull CompletableFuture<Void> setupDefaultSchema();
}
