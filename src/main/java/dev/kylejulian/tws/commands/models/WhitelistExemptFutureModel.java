package dev.kylejulian.tws.commands.models;

import java.util.UUID;

public class WhitelistExemptFutureModel {

    private final UUID playerId;

    private final Boolean isExempt;

    public WhitelistExemptFutureModel(final UUID playerId, final Boolean isExempt) {
        this.playerId = playerId;
        this.isExempt = isExempt;
    }

    /**
     * Get the Player Id
     *
     * @return Player Id
     */
    public UUID getPlayerId() {
        return this.playerId;
    }

    /**
     * Get whether or not the Player was Exempt
     *
     * @return Is auto unwhitelist Exempt
     */
    public Boolean getIsExempt() {
        return this.isExempt;
    }
}
