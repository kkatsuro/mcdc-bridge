package x.dbridge;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class DiscordCommunicatePlayerJoinEvent implements Listener {
    private final JavaPlugin plugin;
    private final @NotNull Logger logger;

    public DiscordCommunicatePlayerJoinEvent(JavaPlugin plugin) {
        this.plugin = plugin;
        logger = plugin.getLogger();
    }
    // unregister it when connection shuts down
    @EventHandler()  // XXX: in which thread this event works?
    public void PlayerJoinEvent(PlayerJoinEvent event) {
        DBridgeChat DBChat = DBridgeChat.getInstance();
        if (DBChat == null) {
            return;
        }

        String nickname = ((TextComponent) event.getPlayer().displayName()).content();
        DBChat.send(String.format("/event\tjoin\t%s", nickname));
    }
}
