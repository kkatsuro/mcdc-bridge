package x.dbridge;
import java.io.BufferedReader;
import java.io.PrintWriter;

import org.bukkit.Bukkit;
import x.commandserver.Exc;

public class DBridgeCommands {
    public static int chat(String[] strings, BufferedReader reader, PrintWriter writer) {
        writer.println("success: starting");
        writer.flush();
        Discbridge plugin = Discbridge.getInstance();
        DBridgeChat DBChat = new DBridgeChat(plugin, reader, writer);
        plugin.getCommand("cords").setExecutor(DBChat);
        plugin.getCommand("cords").setTabCompleter(new CordsTabCompleter());
        DBChat.DiscordForwarder();
        return Exc.DO_NOT_CLOSE;
    }
}
