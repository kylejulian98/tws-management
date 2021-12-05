package dev.kylejulian.twsmanagement.extensions;

import java.util.UUID;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.TablistFormatManager;

import org.jetbrains.annotations.NotNull;

public class TabPluginHelper {

	public static void setTabSuffix(@NotNull UUID playerId, @NotNull String message) {
		TabAPI api = TabAPI.getInstance();
		TabPlayer player = api.getPlayer(playerId);

		if (player == null) {
			// TODO: Log warning
			return;
		}

		TablistFormatManager tablistFormatManager = api.getTablistFormatManager();
		tablistFormatManager.setSuffix(player, message);
	}

	public static boolean hasTabSuffix(@NotNull UUID playerId) {
		TabAPI api = TabAPI.getInstance();
		TabPlayer player = api.getPlayer(playerId);

		if (player == null) {
			return false;
		}

		TablistFormatManager tablistFormatManager = api.getTablistFormatManager();
		String suffix = tablistFormatManager.getCustomSuffix(player);

		return suffix != null && suffix.contains("AFK");
	}
}