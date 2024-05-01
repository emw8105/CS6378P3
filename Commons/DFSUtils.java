package CS6378P3.Commons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
public class DFSUtils {
    public static boolean createFile(String filePath) {
        System.out.println("creating: " + filePath);
        Path path = Paths.get(filePath);
    
        if (Files.exists(path)) {
            return false; // File already exists
        }
        try {
            // Create directories if they don't exist
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            return true;
        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
            return false;
        }
    }
}
