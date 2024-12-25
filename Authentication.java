import java.io.*;
import java.util.*;

public class Authentication {
    private Map<String, String> credentials = new HashMap<>();
    private static final String CREDENTIALS_FILE = "c:/Users/msa29/Desktop/New FTP Server/config/private.txt";

    public Authentication() {
        loadCredentials();
    }

    private void loadCredentials() {
        System.out.println("Loading credentials from: " + CREDENTIALS_FILE);
        File credFile = new File(CREDENTIALS_FILE);

        if (!credFile.exists()) {
            System.err.println("Credentials file not found!");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(credFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = removeBOM(line).trim();
                if (line.isEmpty() || line.startsWith("//")) continue;
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String username = parts[0].trim();
                    String password = parts[1].trim();
                    credentials.put(username, password);
                    System.out.println("Loaded credentials for user: " + username);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading credentials: " + e.getMessage());
        }
    }

    private String removeBOM(String s) {
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    public boolean authenticate(String username, String password) {
        String storedPassword = credentials.get(username);
        System.out.println("Authenticating user: " + username);
        System.out.println("Stored password: " + storedPassword);
        System.out.println("Provided password: " + password);
        return storedPassword != null && storedPassword.equals(password);
    }
}
