package dev.kylejulian.twsmanagement.configuration;

public class HudConfigModel {

    private boolean enabled;
    private Integer refreshRateTicks;

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void setRefreshRateTicks(Integer refreshRateTicks) { this.refreshRateTicks = refreshRateTicks; }

    public boolean getEnabled() { return this.enabled; }

    public Integer getRefreshRateTicks() { return this.refreshRateTicks; }
}
