package tws.management.commands.tabcompleters;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AfkTabCompleter implements TabCompleter {

	private final JavaPlugin plugin;
	
	public AfkTabCompleter(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		ArrayList<String> autoCompleteList = new ArrayList<String>();

        // First arg is a space
        if (args.length == 1) {
            autoCompleteList.add("exempt");
            return autoCompleteList; 
        }

        if (args.length == 2) {
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
