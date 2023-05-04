package x.dbridge;

import org.bukkit.command.CommandSender;

import org.bukkit.command.Command;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CordsTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return new ArrayList<String>(Arrays.asList("type your location here..."));
        }
        return new ArrayList<>();
    }
}