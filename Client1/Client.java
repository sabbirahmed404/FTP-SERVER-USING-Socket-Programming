import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        printWelcomeBanner();
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.print("Username: ");
            String username = scanner.nextLine();
            System.out.print("Password: ");
            String password = scanner.nextLine();

            out.println(username);
            out.println(password);

            String response = in.readLine();
            if ("SUCCESS".equals(response)) {
                System.out.println("Authenticated successfully.");
                String serverResponse;
                while (true) {
                    System.out.print("> ");
                    String command = scanner.nextLine().toLowerCase(); // Make command case-insensitive
                    out.println(command);
                    if (command.startsWith("download")) {
                        receiveFile(command.split(" ")[1], socket);
                    } else if (command.startsWith("upload")) {
                        uploadFile(command.split(" ")[1], socket, in, out);
                    } else if (command.startsWith("showfiles")) {
                        showFiles(in);
                    } else if (command.startsWith("search")) {
                        searchFile(in);
                    } else {
                        while (!(serverResponse = in.readLine()).equals("END")) {
                            System.out.println(serverResponse);
                        }
                    }
                }
            } else {
                System.out.println("Authentication failed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printWelcomeBanner() {
        System.out.println("=========================================");
        System.out.println(" Welcome to the FTP Using Java Socket Programming ");
        System.out.println(" Developed by: ");
        System.out.println(" Sabbir Ahmed, Raihan Kabir, Ramjan Ali ");
        System.out.println(" Green University of Bangladesh ");
        System.out.println("=========================================");
    }

    private static void receiveFile(String fileName, Socket socket) {
        try {
            // We rely on the same streams passed to main() (in and out), but for clarity:
            InputStream is = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            // Wait for the server to send "BEGIN_FILE_TRANSFER"
            String line = reader.readLine();
            if (!"BEGIN_FILE_TRANSFER".equals(line)) {
                System.out.println("Server did not send BEGIN_FILE_TRANSFER.");
                return;
            }

            // Open the destination file
            try (FileOutputStream fos = new FileOutputStream(fileName);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                byte[] buffer = new byte[4096];
                boolean done = false;

                // Keep reading until we detect "END_FILE_TRANSFER"
                while (!done) {
                    // If server sends a text line, check if it's the end marker
                    if (reader.ready()) {
                        // Read a line from text stream
                        String maybeMarker = reader.readLine();
                        if ("END_FILE_TRANSFER".equals(maybeMarker)) {
                            done = true;
                            break;
                        }
                        // Otherwise we ignore unexpected lines during transfer
                    }

                    // Read data in blocking fashion
                    int bytesRead = is.read(buffer);
                    if (bytesRead < 0) {
                        // Socket closed unexpectedly
                        break;
                    }
                    if (bytesRead > 0) {
                        bos.write(buffer, 0, bytesRead);
                    }
                }
                bos.flush();
                System.out.println("File downloaded successfully.");
            }

            // Finally, wait for the single "END" line
            while ((line = reader.readLine()) != null) {
                if ("END".equals(line)) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error downloading file: " + e.getMessage());
        }
    }

    private static void uploadFile(String fileName, Socket socket,
                                   BufferedReader in, PrintWriter out) {
        File localFile = new File(fileName);
        if (!localFile.exists() || !localFile.isFile()) {
            System.out.println("Local file not found: " + fileName);
            return;
        }
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(localFile))) {
            // Wait for server to confirm it's ready
            String serverLine = in.readLine();
            if (!"BEGIN_FILE_UPLOAD".equals(serverLine)) {
                System.out.println("Server did not accept upload.");
                return;
            }
            // Send file data
            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            // Signal the end of upload
            out.println("END_FILE_UPLOAD");
            out.flush();
            // Print server responses until "END"
            String line;
            while (!(line = in.readLine()).equals("END")) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error uploading file: " + e.getMessage());
        }
    }

    private static void showFiles(BufferedReader in) throws IOException {
        String line;
        while (!(line = in.readLine()).equals("END")) {
            System.out.println(line);
        }
    }

    private static void searchFile(BufferedReader in) throws IOException {
        String line;
        while (!(line = in.readLine()).equals("END")) {
            System.out.println(line);
        }
    }
}
