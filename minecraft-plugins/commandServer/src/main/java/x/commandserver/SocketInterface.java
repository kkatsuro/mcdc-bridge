package x.commandserver;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Logger;

public class SocketInterface extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final CommandServerExecutor executor;

    public SocketInterface(JavaPlugin plugin) {
        this.plugin = plugin;
        logger = plugin.getLogger();
        executor = CommandServerExecutor.getInstance();
    }

    public void run() {
        logger.info("STARTING LISTENING AT PORT 7551");
        try {
            ServerSocket serverSock = new ServerSocket(7551);  // TODO: load port number from config
            while (true) {
                new SocketConnection(plugin, serverSock.accept()).runTaskAsynchronously(plugin);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
