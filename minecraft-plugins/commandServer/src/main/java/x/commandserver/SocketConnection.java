package x.commandserver;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.logging.Logger;

public class SocketConnection extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final CommandServerExecutor executor;
    private final Socket sock;

    public SocketConnection(JavaPlugin plugin, Socket sock) {
        this.plugin = plugin;
        this.sock = sock;
        logger = plugin.getLogger();
        executor = CommandServerExecutor.getInstance();
    }

    public void run() {
        try {
            // sock.setSoTimeout(3000);  // I can't actually do this, right? TODO: register option to set no timeout
            InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
            BufferedReader reader = new BufferedReader(streamReader);
            PrintWriter writer = new PrintWriter(sock.getOutputStream());
            logger.info("socket 7551: new connection");
            int returnCode = 0;
            try {
                String[] split = reader.readLine().split(" ");  // TODO: empty string situation
                String command = split[0];
                String[] arguments = Arrays.copyOfRange(split, 1, split.length);
                returnCode = executor.execute(command, arguments, reader, writer);
                logger.info(String.format("command %s returned returnCode %d", command, returnCode));
                if (returnCode == Exc.NOT_REGISTERED) {
                    writer.println("error: command not registered");
                }
            } catch( SocketTimeoutException ex) {
                logger.info("SocketTimeoutException");
                writer.println("error: timeout");
            } finally {
                if (returnCode != Exc.DO_NOT_CLOSE) {
                    writer.close();
                    sock.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
