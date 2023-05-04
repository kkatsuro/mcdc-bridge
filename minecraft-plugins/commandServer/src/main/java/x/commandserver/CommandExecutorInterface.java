package x.commandserver;

import java.io.BufferedReader;
import java.io.PrintWriter;

public interface CommandExecutorInterface {
    int execute(String[] args, BufferedReader reader, PrintWriter writer);
}
