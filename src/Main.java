import java.util.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.File;
import java.io.FileReader;


public class Main {
    public static void main(String[] args) {
        System.out.println("HTTP Server start");

        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientsocket = serverSocket.accept();
                System.out.println("Initiated new connection.");
                new Thread(() -> handleRequest(clientsocket)).start(); // Multi-threading
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }


    public static void handleRequest(Socket clientsocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientsocket.getInputStream()));

            //get the request to further dissect it
            String requestLine = reader.readLine();
            if (requestLine == null) {
                clientsocket.close();
                return;
            }

            System.out.println("Received request: " + requestLine);

            //Split the request to process the request
            String[] parts = requestLine.split(" ");
            String method = parts[0];
            String path = (parts.length > 1) ? parts[1] : "/";

            // Extract query parameters
            String[] pathParts = path.split("\\?", 2);
            String route = pathParts[0];
            String queryString = (pathParts.length > 1) ? pathParts[1] : null;

            Map<String, String> queryParams = new HashMap<>();
            if (queryString != null) {
                for (String param : queryString.split("&")) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        queryParams.put(keyValue[0], keyValue[1]);
                    }
                }
            }

            System.out.println("query string: " + queryParams);

            OutputStream outputStream = clientsocket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true);

            // default files for certain routes
            if (route.equals("/")) {
                route = "/index.html";
            } else if (route.equals("/about")) {
                route = "/about.html";
            }

            // Serve static HTML files
            File rootDir = new File(System.getProperty("user.dir"), "src");
            File file = new File(rootDir, "public" + File.separator + route);

            if (file.exists() && !file.isDirectory()) {
                BufferedReader fileReader = new BufferedReader(new FileReader(file));
                String fileLine;

                writer.write("HTTP/1.1 200 OK\r\n");
                writer.write("Content-Type: text/html\r\n\r\n");

                while ((fileLine = fileReader.readLine()) != null) {
                    writer.write(fileLine + "\n");
                }
                fileReader.close();
            } else {
                // Serve custom 404 page
                File notFoundFile = new File(rootDir, "public" + File.separator + "404.html");
                if (notFoundFile.exists()) {
                    BufferedReader fileReader = new BufferedReader(new FileReader(notFoundFile));
                    String fileLine;

                    writer.write("HTTP/1.1 404 Not Found\r\n");
                    writer.write("Content-Type: text/html\r\n\r\n");

                    while ((fileLine = fileReader.readLine()) != null) {
                        writer.write(fileLine + "\n");
                    }
                    fileReader.close();
                } else {
                    // Fallback if `404.html` is missing
                    writer.write("HTTP/1.1 404 Not Found\r\n\r\nPage not found.");
                }
            }

            writer.flush();
            clientsocket.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

//create a switch case for different methods
// handle different server methods with appropriate methods