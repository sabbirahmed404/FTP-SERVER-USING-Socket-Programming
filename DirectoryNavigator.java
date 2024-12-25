import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirectoryNavigator {
    private Path currentDir;

    public DirectoryNavigator(String rootDir) {
        this.currentDir = Paths.get(rootDir).toAbsolutePath().normalize();
    }

    public String listFiles() {
        StringBuilder sb = new StringBuilder();
        File dir = currentDir.toFile();
        for (File file : dir.listFiles()) {
            sb.append(file.getName()).append("\n");
        }
        return sb.toString();
    }

    public String changeDirectory(String dir) {
        Path newPath = currentDir.resolve(dir).normalize();
        if (newPath.startsWith(currentDir.getRoot()) && newPath.toFile().isDirectory()) {
            currentDir = newPath;
            return "Changed directory to " + currentDir;
        } else {
            return "Invalid directory";
        }
    }

    public String printWorkingDirectory() {
        return currentDir.toString();
    }
}
