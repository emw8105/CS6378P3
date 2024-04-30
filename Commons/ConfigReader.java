package CS6378P3.Commons;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigReader {
    public static List<Node> readConfigFile(String filePath) {
        List<Node> nodes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignore comment lines starting with #
                if (!line.trim().startsWith("#")) {
                    // Split the line by spaces
                    String[] parts = line.trim().split("\\s+");
                    // Ensure the line has at least 3 parts (uid, hostname, port)
                    if (parts.length >= 3) {
                        try {
                            int uid = Integer.parseInt(parts[0]);
                            String hostname = parts[1];
                            int port = Integer.parseInt(parts[2]);
                            // Create and add a new Node object to the list
                            nodes.add(new Node(uid, hostname, port));
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid line format: " + line);
                        }
                    } else {
                        System.err.println("Invalid line format: " + line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nodes;
    }
}
