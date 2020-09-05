package dev.kylejulian.twsmanagement.server.whitelist;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerWhitelisted {

    private final UUID playerId;
    private Boolean whitelist;

    public PlayerWhitelisted(@NotNull UUID playerId, Boolean whitelist) {
        this.playerId = playerId;
        this.whitelist = whitelist;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setWhitelist(Boolean whitelist) {
        this.whitelist = whitelist;
    }

    public Boolean getWhitelist() {
        return whitelist;
    }
}
