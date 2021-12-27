package dev.kylejulian.twsmanagement.configuration;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.bukkit.plugin.java.JavaPlugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

public class ConfigurationManager {

    private ConfigModel config;
    private final File file;
    private final JavaPlugin plugin;

    private final String[] defaultAfkEvents = new String[]{"onPlayerChat",
            "onPlayerInteractEntity",
            "onPlayerInteract",
            "onPlayerBedEnter",
            "onPlayerChangedWorld",
            "onPlayerEditBook",
            "onPlayerDropItem",
            "onPlayerItemBreak",
            "onPlayerShearEntity",
            "onPlayerToggleFlight",
            "onPlayerToggleSprint",
            "onPlayerToggleSneak",
            "onPlayerUnleashEntity",
            "onPlayerBucketFill",
            "onPlayerBucketEmpty",
            "onPlayerMove",
            "onPlayerExpChange"};

    public ConfigurationManager(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(this.plugin.getDataFolder(), fileName);
    }

    /**
     * Reloads the Configuration File
     */
    public void reload() {
        if (!this.file.getParentFile().exists()) {
            boolean makeDirectoryResult = this.file.getParentFile().mkdirs();
            if (!makeDirectoryResult) {
                this.plugin.getLogger().log(Level.WARNING, "Unable to make directory");
                return;
            }
        }

        if (!this.file.exists()) {
            this.plugin.saveResource(this.file.getName(), false);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            this.config = mapper.readValue(this.file, ConfigModel.class);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Unable to read Configuration file!");
            this.plugin.getLogger().log(Level.SEVERE, e.getMessage());
        }

        boolean saveRequired = verifyConfigurationDefaults(this.config);

        if (saveRequired) {
            try {
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(this.file, this.config);
            } catch (IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Unable to write to Configuration file!");
                this.plugin.getLogger().log(Level.SEVERE, e.getMessage());
            }
        }
    }

    private boolean verifyConfigurationDefaults(@NotNull ConfigModel configModel) {
        boolean saveRequired = false;

        // Set default Afk configuration
        if (configModel.getAfkConfig() == null) {
            AfkConfigModel afkConfigModel = new AfkConfigModel();
            afkConfigModel.setEvents(this.defaultAfkEvents);
            afkConfigModel.setKickMessage("You have been kicked for being AFK for too long!");
            afkConfigModel.setKickTimeMinutes(5);
            afkConfigModel.setPlayerCountNeededForKick(25);
            afkConfigModel.setTimeMinutes(5);
            afkConfigModel.setSendPlayerAfkMessage(true);
            afkConfigModel.setAfkKick(true);

            configModel.setAfkConfig(afkConfigModel);
            saveRequired = true;
        }

        // Set default Night Reset configuration
        if (configModel.getNightResetConfig() == null) {
            NightResetConfigModel nightResetConfigModel = new NightResetConfigModel();
            nightResetConfigModel.setEnabled(true);

            configModel.setNightResetConfig(nightResetConfigModel);
            saveRequired = true;
        }

        // Set default Hud configuration
        if (configModel.getHudConfig() == null) {
            HudConfigModel hudConfigModel = new HudConfigModel();
            hudConfigModel.setEnabled(true);
            hudConfigModel.setRefreshRateTicks(20); // Every second

            configModel.setHudConfig(hudConfigModel);
            saveRequired = true;
        }

        // Set default Whitelist configuration
        if (configModel.getWhitelistConfig() == null) {
            WhitelistConfigModel whitelistConfigModel = new WhitelistConfigModel();
            whitelistConfigModel.setEnabled(true);
            whitelistConfigModel.setInactivity(Duration.ofDays(14));
            whitelistConfigModel.setCheck(Duration.ofMinutes(30));
            whitelistConfigModel.setWriteLogFile(true);

            configModel.setWhitelistConfig(whitelistConfigModel);
            saveRequired = true;
        }

        // Set default Database configuration
        if (configModel.getDatabaseConfig() == null) {
            DatabaseConfigModel databaseConfigModel = new DatabaseConfigModel();
            databaseConfigModel.setName("tws-local.db");
            databaseConfigModel.setMaxConcurrentConnections(5);

            configModel.setDatabaseConfig(databaseConfigModel);
            saveRequired = true;
        }

        return saveRequired;
    }

    /**
     * Get the current Configuration
     *
     * @return Configuration
     */
    public ConfigModel getConfig() {
        return this.config;
    }
}
