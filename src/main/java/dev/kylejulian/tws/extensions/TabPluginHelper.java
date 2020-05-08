package dev.kylejulian.tws.extensions;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

public class TabPluginHelper {

	public static void setTabSuffix(JavaPlugin plugin, UUID playerId, String message) {
		try {
			Class.forName("me.neznamy.tab.api.TABAPI");
			me.neznamy.tab.api.EnumProperty type = me.neznamy.tab.api.EnumProperty.TABSUFFIX;
			me.neznamy.tab.api.TABAPI.setValueTemporarily(playerId, type, message);
		} catch (ClassNotFoundException e) {
			plugin.getServer().getLogger().log(Level.WARNING, "Unable to set TAB Suffix for Player [" + playerId + "]");
			plugin.getServer().getLogger().log(Level.WARNING, e.getMessage());
		}
	}
}
