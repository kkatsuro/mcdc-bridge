package x.whitelist;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

public final class WhitelistTres extends JavaPlugin {
    private final Logger logger = getLogger();
    private static WhitelistTres plugin;

    @Override
    public void onEnable() {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("CommandServer")) {
            logger.info("CommandServer plugin disabled");  // can this even happen if CommandServer is dependency?
            return;
        }
        plugin = this;
        BukkitTask task = new WhitelistPreLoginEvent(this).runTaskAsynchronously(plugin);
        logger.info("started WhitelistPreLoginEvent task");
    }

    public static WhitelistTres getInstance() {
        return plugin;
    }
}
