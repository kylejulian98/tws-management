package dev.kylejulian.tws.configuration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigurationManager {

	private ConfigModel config;
	private final File file;
	private final JavaPlugin plugin;
	
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
			plugin.saveResource(this.file.getName(), false);
		}
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			this.config = mapper.readValue(this.file, ConfigModel.class);
		} catch (IOException e) {
			this.plugin.getLogger().log(Level.SEVERE, "Unable to read Configuration file!");
		}
	}
	
	/**
	 * Get the current Configuration
	 * @return Configuration
	 */
	public ConfigModel getConfig() {
		return this.config;
	}
}
