package dev.kylejulian.twsmanagement.extensions;

import java.util.UUID;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.jetbrains.annotations.NotNull;

public class TabPluginHelper {

	public static void setTabSuffix(@NotNull UUID playerId, @NotNull TextComponent suffix) {
		TabAPI api = TabAPI.getInstance();
		TabPlayer player = api.getPlayer(playerId);

		if (player == null) {
			// TODO: Log warning
			return;
		}

		TabListFormatManager tablistFormatManager = api.getTabListFormatManager();

		MiniMessage mm = MiniMessage.miniMessage();
		String suffixText =  mm.serialize(suffix);

		tablistFormatManager.setSuffix(player, " &8[&cAFK&8]");
	}

	public static void resetTabSuffix(@NotNull UUID playerId) {
		TabAPI api = TabAPI.getInstance();
		TabPlayer player = api.getPlayer(playerId);

		if (player == null) {
			// TODO: Log warning
			return;
		}

		TabListFormatManager tablistFormatManager = api.getTabListFormatManager();
		tablistFormatManager.setSuffix(player, "");
	}

	public static boolean hasTabSuffix(@NotNull UUID playerId) {
		TabAPI api = TabAPI.getInstance();
		TabPlayer player = api.getPlayer(playerId);

		if (player == null) {
			return false;
		}

		TabListFormatManager tablistFormatManager = api.getTabListFormatManager();
		String suffix = tablistFormatManager.getCustomSuffix(player);

		return suffix != null && suffix.contains("AFK");
	}
}