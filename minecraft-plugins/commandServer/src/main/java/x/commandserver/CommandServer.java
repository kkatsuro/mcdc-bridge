package x.commandserver;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.logging.Logger;

public final class CommandServer extends JavaPlugin {
    private final Logger logger = getLogger();
    private static CommandServer plugin;
    private static CommandServerExecutor executor;

    @Override
    public void onEnable() {
        plugin = this;
        BukkitTask task = new SocketInterface(plugin).runTaskAsynchronously(plugin);
        logger.info("started SocketInterface task");
        executor = CommandServerExecutor.getInstance();
        executor.register("aaaa", CommandServer::aaaa);
    }

    public static CommandServer getInstance() { return plugin; }
    public static CommandServerExecutor  getExecutor() { return CommandServerExecutor.getInstance(); }

    public static int aaaa(String[] strings, BufferedReader reader, PrintWriter writer) {
        CommandServer instance = CommandServer.getInstance();
        instance.logger.info("AAAAa");
        Bukkit.broadcastMessage("AAAAa");
        writer.println("success: broadcasted AAAAa");
        return 0;
    }
}
