package x.dbridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import x.commandserver.CommandServer;
import x.commandserver.CommandServerExecutor;

import java.util.logging.Logger;

public final class Discbridge extends JavaPlugin {
    private static Discbridge plugin;
    private final Logger logger = getLogger();
    CommandServerExecutor executor;

    @Override
    public void onEnable() {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("CommandServer")) {
            logger.info("CommandServer plugin disabled");  // can this even happen if CommandServer is dependency?
            return;
        }
        plugin = this;

        CommandServer cs = (CommandServer) Bukkit.getServer().getPluginManager().getPlugin("CommandServer");
        executor = cs.getExecutor();
        executor.register("chat", DBridgeCommands::chat);
        logger.info("registered chat command");

        plugin.getServer().getPluginManager().registerEvents(new DBridgeAsyncChatEvent(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DiscordCommunicatePlayerJoinEvent(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DiscordCommunicatePlayerQuitEvent(plugin), plugin);

        // this is place where we might want to have server command registration,
        // but it is in DBridgeCommands now, since DBridgeCommands runs in different thread
        // which may actually do not matter
        // Also, /cords works only in chat is connected, so we shouldn't register it at start actually
    }

    @Override
    public void onDisable() {
        // TODO
    }

    public static Discbridge getInstance() {
        return plugin;
    }
}
