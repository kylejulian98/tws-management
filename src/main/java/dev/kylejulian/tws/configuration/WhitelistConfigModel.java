package dev.kylejulian.tws.configuration;

import org.jetbrains.annotations.NotNull;

public class WhitelistConfigModel {

    private boolean enabled;
    private Integer days;
    private Integer hours;

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void setDays(@NotNull Integer days) { this.days = days; }

    public void setHours(@NotNull Integer hours) { this.hours = hours; }

    public boolean getEnabled() { return this.enabled; }

    public @NotNull Integer getDays() { return this.days; }

    public @NotNull Integer getHours() { return this.hours; }

}
