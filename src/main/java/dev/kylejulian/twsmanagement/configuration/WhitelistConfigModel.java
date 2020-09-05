package dev.kylejulian.twsmanagement.configuration;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class WhitelistConfigModel {

    private boolean enabled;
    private Duration inactivity, check;

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void setInactivity(@NotNull Duration duration) { this.inactivity = duration; }

    /**
     * Sets how often the Whitelist check is performed
     * @param duration
     */
    public void setCheck(@NotNull Duration duration) { this.check = duration; }

    public boolean getEnabled() { return this.enabled; }

    public @NotNull Duration getInactivity() { return this.inactivity; }

    /**
     * How often the Whitelist check is performed
     * @return Duration between checks
     */
    public @NotNull Duration getCheck() { return this.check; }
}
