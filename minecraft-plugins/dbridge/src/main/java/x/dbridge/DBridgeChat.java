package x.dbridge;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class DBridgeChat implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private static DBridgeChat instance;

    public static DBridgeChat getInstance() {
        return instance;
    }

    public DBridgeChat(JavaPlugin plugin, BufferedReader reader, PrintWriter writer) {
        this.plugin = plugin;
        logger = plugin.getLogger();
        this.reader = reader;
        this.writer = writer;
        instance = this;
    }

    public void send(String text) {
        writer.println(text);
        writer.flush();
    }

    // do we run commands here?
    // or do we just forward discord here...
    public void DiscordForwarder() {
        String line;
        while (true) {
            try {
                line = reader.readLine();
                if (line == null) {
                    // close everything ??
                    return;
                }
                Bukkit.broadcastMessage(line);
            } catch (IOException e) {
                logger.info("Exception in DiscordForwarder");
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        logger.info("/cords command used");
        // Bukkit.broadcastMessage("used /cords");  TODO: send 'saved to discord' dm
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;
        String nickname = ((TextComponent) player.displayName()).content();
        Location l = player.getLocation();
        String message;
        if (args.length > 0) {
            message = String.join(" ", args);
        } else {
            message = "None";
        }
        if (message.contains("\t")) {  // you technically can't send \t with minecraft, but..
            player.sendMessage("i am  very sorry sir you cant use \\t in yuor command mmessages sir");
            return false;
        }
        send(String.format("/cords\t%s\t%s\t%s\t%s\t%s", nickname, (int)l.x(), (int)l.y(), (int)l.z(), message));
        return true;
    }
}
