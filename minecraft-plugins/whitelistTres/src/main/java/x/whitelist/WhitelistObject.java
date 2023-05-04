package x.whitelist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class WhitelistObject {
    private static WhitelistObject instance = null;
    private HashMap<String, HashSet<String>> hashmap = new HashMap<>();
    String filename;

    protected WhitelistObject() {
        this.filename = "ip_whitelist.ipw";  // TODO: read this from config
        readFile();
    }

    public static WhitelistObject getInstance() {
        if (instance == null) {
            instance = new WhitelistObject();
        }
        return instance;
    }

    public boolean userWhitelisted(String username) {
        return (hashmap.get(username) != null);
    }

    public boolean userWhitelisted(String username, String ip_address) {
        HashSet<String> ips = hashmap.get(username);
        if (ips == null) {
            return false;
        }
        return ips.contains(ip_address);
    }

    public void readFile() {
        createIfNotExists();
        Scanner sc;
        try {
            sc = new Scanner(new File(filename));
        } catch (FileNotFoundException e) { return; }

        HashMap<String, HashSet<String>> temp_hashmap = new HashMap<>();
        while (sc.hasNextLine()) {
            String[] line = sc.nextLine().split("\\s+");
            temp_hashmap.put(line[0],
                    new HashSet<>( Arrays.asList( Arrays.copyOfRange(line, 1, line.length) ) )
            );
        }
        hashmap = temp_hashmap;
    }

    private void saveFile() {
        try {
            FileWriter fWriter = new FileWriter(filename);
            for (Map.Entry<String, HashSet<String>> entry : hashmap.entrySet()) {
                String username = entry.getKey();
                HashSet<String> ips = entry.getValue();
                Iterator<String> it = ips.iterator();
                fWriter.write(username);
                while (it.hasNext()) {
                    fWriter.write(" " + it.next());
                }
                fWriter.write("\n");
            }
            fWriter.close();
        }
        catch (IOException e) {
            // logger.info(e.getMessage());
        }
    }

    private void createIfNotExists() {
        try {
            File myObj = new File(filename);
            myObj.createNewFile();
        } catch (IOException e) {
            // logger.info("An error occurred."); TODO: logger?
            // e.printStackTrace();
        }
    }
}
