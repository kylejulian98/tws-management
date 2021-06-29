package dev.kylejulian.twsmanagement.extensions;

import java.util.UUID;

import me.neznamy.tab.api.EnumProperty;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.TABAPI;
import org.jetbrains.annotations.NotNull;

public class TabPluginHelper {

	public static void setTabSuffix(@NotNull UUID playerId, @NotNull String message) {
		EnumProperty type = EnumProperty.TABSUFFIX;
		TabPlayer player = TABAPI.getPlayer(playerId);
		player.setValueTemporarily(type, message);
	}

	public static boolean hasTabSuffix(@NotNull UUID playerId) {
		EnumProperty type = EnumProperty.TABSUFFIX;
		TabPlayer player = TABAPI.getPlayer(playerId);
		String suffix = player.getTemporaryValue(type);

		return suffix != null && suffix.contains("AFK");
	}
}
