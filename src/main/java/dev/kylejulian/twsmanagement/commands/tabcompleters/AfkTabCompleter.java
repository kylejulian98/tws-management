package dev.kylejulian.twsmanagement.commands.tabcompleters;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public record AfkTabCompleter(JavaPlugin plugin) implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String[] args) {

        ArrayList<String> autoCompleteList = new ArrayList<>();

        // First arg is a space
        if (args.length == 1) {
            autoCompleteList.add("exempt");
            return autoCompleteList;
        }

        if (args.length == 2) {
            autoCompleteList.add("add");
            autoCompleteList.add("remove");
            autoCompleteList.add("list");
            autoCompleteList.add("clear");
            return autoCompleteList;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            String playerName = PlainTextComponentSerializer.plainText().serialize(player.displayName());
            autoCompleteList.add(playerName);
        }

        return autoCompleteList;
    }
}
