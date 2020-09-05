package dev.kylejulian.twsmanagement.commands.tabcompleters;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WhitelistExemptTabCompleter implements TabCompleter {

    private final JavaPlugin plugin;

    public WhitelistExemptTabCompleter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        ArrayList<String> autoCompleteList = new ArrayList<>();

        if (args.length == 1) {
            autoCompleteList.add("add");
            autoCompleteList.add("remove");
            autoCompleteList.add("list");
            return autoCompleteList;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            autoCompleteList.add(player.getDisplayName());
        }

        return autoCompleteList;
    }
}
