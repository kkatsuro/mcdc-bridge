package x.whitelist;

import x.commandserver.Exc;
import java.io.BufferedReader;
import java.io.PrintWriter;

public class WhitelistCommands {
    public static int reload(String[] strings, BufferedReader reader, PrintWriter writer) {
        WhitelistObject whitelist = WhitelistObject.getInstance();
        whitelist.readFile();
        writer.println("success: reloaded");  // TODO: create interface for writer.println ?
        WhitelistTres instance = WhitelistTres.getInstance();
        instance.getLogger().info("reloaded whitelist file");
        return Exc.SUCCESS;
    }
}
