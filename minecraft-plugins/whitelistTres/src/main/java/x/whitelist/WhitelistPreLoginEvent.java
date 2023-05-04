package x.whitelist;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import x.commandserver.CommandServer;
import x.commandserver.CommandServerExecutor;

import java.util.logging.Logger;

public class WhitelistPreLoginEvent extends BukkitRunnable implements Listener {
    private final JavaPlugin plugin;
    private final Logger logger;
    private WhitelistObject whitelist;
    CommandServerExecutor executor;

    public WhitelistPreLoginEvent(JavaPlugin plugin) {
        this.plugin = plugin;
        logger = plugin.getLogger();
    }

    // run happens when class already is in new thread, thats why we don't want
    // unneccessary code inside constructor which still happens in main thread
    public void run() {
        logger.info("WhitelistPreloginEvent run() function");
        {
            long start = System.currentTimeMillis();
            whitelist = WhitelistObject.getInstance();
            long finish = System.currentTimeMillis();
            logger.info("parsed whitelist file in " + (finish - start) + " miliseconds");
        }

        CommandServer cs = (CommandServer) Bukkit.getServer().getPluginManager().getPlugin("CommandServer");
        executor = cs.getExecutor();
        executor.register("reload", WhitelistCommands::reload);

        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler()
    public void AsyncPlayerPreLoginEvent(AsyncPlayerPreLoginEvent event) {
        String username = event.getName();
        String user_ip = event.getAddress().toString().substring(1);  // TODO: is this really with slash prefix?
        if (!whitelist.userWhitelisted(username)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                           "Username " + username + " is not whitelisted");
            return;
        };

        if (whitelist.userWhitelisted(username, user_ip)) {
            return;  // connection successful
        }

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                      "IP " + user_ip + " is not whitelisted for "
                      + username + " username");
    }
}
