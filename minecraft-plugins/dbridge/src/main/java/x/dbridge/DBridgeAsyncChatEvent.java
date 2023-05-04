package x.dbridge;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class DBridgeAsyncChatEvent implements Listener {
    private final JavaPlugin plugin;
    private final Logger logger;

    public DBridgeAsyncChatEvent (JavaPlugin plugin) {
        this.plugin = plugin;
        logger = plugin.getLogger();
    }
    // unregister it when connection shuts down
    @EventHandler()  // XXX: in which thread this event works?
    public void AsyncChatEvent(AsyncChatEvent event) {
        logger.info("async chat event fired");
        DBridgeChat DBChat = DBridgeChat.getInstance();
        if (DBChat == null) {
            return;
        }
        // TODO: check if user haven't disabled discord forwarding
        String nickname = ((TextComponent) event.getPlayer().displayName()).content();
        logger.info(String.format("user %s uses /cords commands!", nickname));
        if (nickname.equals("")) {
            logger.info("Empty username in async chat event!");
            return;
        }
        String message = ((TextComponent) event.message()).content();
        DBChat.send(String.format("%s %s", nickname, message));
    }
}
