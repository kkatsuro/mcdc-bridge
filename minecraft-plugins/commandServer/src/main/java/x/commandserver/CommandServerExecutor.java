package x.commandserver;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class CommandServerExecutor {
    public Map<String, CommandExecutorInterface> commands;
    private static CommandServerExecutor instance = null;
    protected CommandServerExecutor() {
        commands = new HashMap<>();
    }

    public static CommandServerExecutor getInstance() {
        if (instance == null) {
            instance = new CommandServerExecutor();
        }
        return instance;
    }

    public void register(String command, CommandExecutorInterface function) {
        commands.put(command, function);
    }

    public int execute(String command, String[] arguments, BufferedReader reader, PrintWriter writer) {
        CommandExecutorInterface CEInterface = commands.get(command);
        if (CEInterface != null) {
            return CEInterface.execute(arguments, reader, writer);
        }
        return Exc.NOT_REGISTERED;
    }
}
