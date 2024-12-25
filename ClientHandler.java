import java.io.*;
import java.net.*;
import java.nio.file.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String storageDir;
    private Authentication auth;
    private BufferedReader in;
    private PrintWriter out;
    private Path currentDir;

    public ClientHandler(Socket clientSocket, String storageDir) {
        this.clientSocket = clientSocket;
        this.storageDir = storageDir;
        this.auth = new Authentication();
        this.currentDir = Paths.get(storageDir);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String username = in.readLine();
            String password = in.readLine();

            if (auth.authenticate(username, password)) {
                out.println("SUCCESS");
                sendHelp();
                handleClientCommands();
            } else {
                out.println("FAILURE");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Comment out or remove the socket close line:
            // clientSocket.close();
        }
    }

    private void handleClientCommands() throws IOException {
        while (true) {
            String command = in.readLine();
            if (command == null) {
                break; // Exit if client disconnects
            }
            command = command.toLowerCase(); // Make command case-insensitive
            if (command.startsWith("ls")) {
                listFiles();
            } else if (command.startsWith("cd")) {
                changeDirectory(command);
            } else if (command.startsWith("pwd")) {
                printWorkingDirectory();
            } else if (command.startsWith("upload")) {
                uploadFile(command);
            } else if (command.startsWith("download")) {
                downloadFile(command);
            } else if (command.startsWith("showfiles")) {
                showFiles(command);
            } else if (command.startsWith("search")) {
                searchFile(command);
            } else if (command.startsWith("help")) {
                sendHelp();
            } else {
                out.println("Invalid command");
            }
            out.println("END");
        }
    }

    private void listFiles() {
        File dir = currentDir.toFile();
        for (File file : dir.listFiles()) {
            out.println(file.getName());
        }
    }

    private void changeDirectory(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 2) {
            Path newPath = currentDir.resolve(parts[1]).normalize();
            if (newPath.startsWith(storageDir) && Files.isDirectory(newPath)) {
                currentDir = newPath;
                out.println("Changed directory to " + currentDir);
            } else {
                out.println("Invalid directory");
            }
        } else {
            out.println("Usage: cd <directory>");
        }
    }

    private void printWorkingDirectory() {
        out.println(currentDir.toString());
    }

    private void downloadFile(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 2) {
            Path filePath = currentDir.resolve(parts[1]).normalize();
            if (filePath.startsWith(storageDir) && Files.exists(filePath) && !Files.isDirectory(filePath)) {
                try {
                    // Signal start of transfer
                    out.println("BEGIN_FILE_TRANSFER");
                    
                    // Send file data
                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath.toFile()))) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        OutputStream os = clientSocket.getOutputStream();
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        os.flush(); // Single flush at end
                    }
                    
                    // Signal end of transfer
                    out.println("END_FILE_TRANSFER");
                    // Follow up with the normal "END" marker so the client returns to its prompt
                    out.println("END");
                } catch (IOException e) {
                    out.println("Error: " + e.getMessage());
                    out.println("END");
                }
            } else {
                out.println("File not found or is a directory");
                out.println("END");
            }
        } else {
            out.println("Usage: download <file>");
            out.println("END");
        }
    }

    private void uploadFile(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 2) {
            String fileName = parts[1];
            Path filePath = currentDir.resolve(fileName).normalize();
            try {
                // Signal client to start sending the file
                out.println("BEGIN_FILE_UPLOAD");
                
                try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    
                    InputStream is = clientSocket.getInputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    
                    // Read raw file bytes until none are available
                    while ((bytesRead = is.read(buffer)) > 0) {
                        bos.write(buffer, 0, bytesRead);
                    }
                    bos.flush();
                }
                
                // Now wait for the client's "END_FILE_UPLOAD" marker
                String marker = in.readLine();
                if ("END_FILE_UPLOAD".equals(marker)) {
                    out.println("File upload complete: " + fileName);
                } else {
                    out.println("Upload ended unexpectedly (missing END_FILE_UPLOAD)");
                }
            } catch (IOException e) {
                out.println("Error uploading file: " + e.getMessage());
            }
        } else {
            out.println("Usage: upload <file>");
        }
    }

    private void showFiles(String command) {
        String[] parts = command.split(" ");
        String subdirectory = parts.length > 1 ? parts[1].toLowerCase() : "";
        try (BufferedReader reader = new BufferedReader(new FileReader("c:/Users/msa29/Desktop/New FTP Server/config/list.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (subdirectory.isEmpty() || line.toLowerCase().contains("\\" + subdirectory + "\\")) {
                    String fileName = line.substring(line.lastIndexOf("\\") + 1);
                    out.println("File: " + fileName);
                }
            }
        } catch (IOException e) {
            out.println("Error reading file list: " + e.getMessage());
        }
    }

    private void searchFile(String command) {
        String[] parts = command.split(" ");
        if (parts.length == 2) {
            String fileName = parts[1];
            String result = Search.searchFile(fileName);
            out.println(result);
        } else {
            out.println("Usage: search <file>");
        }
    }

    private void sendHelp() {
        out.println("Available commands:");
        out.println("ls - List files and directories");
        out.println("cd <directory> - Change directory");
        out.println("pwd - Print working directory");
        out.println("upload <file> - Upload a file to the server");
        out.println("download <file> - Download a file from the server");
        out.println("showfiles - Show contents of list.txt");
        out.println("search <file> - Search for a file");
        out.println("help - Show this help message");
    }
}
