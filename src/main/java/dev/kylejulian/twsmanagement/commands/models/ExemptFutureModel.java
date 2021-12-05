package dev.kylejulian.twsmanagement.commands.models;

import java.util.UUID;

public record ExemptFutureModel(UUID playerId, Boolean isExempt) {

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
