package com.psd;

import com.psd.entities.*;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Represents a P2P server that listens for incoming messages from peers.
 * Each user has their own P2PServer running, which listens on the specified port.
 */
public class P2PServer {

    private int port;  // The port on which the server listens
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private BorderPane mainMenuLayout;  // Reference to the main layout in the JavaFX UI

    public P2PServer(int port, BorderPane mainMenuLayout) {
        this.port = port;
        this.mainMenuLayout = mainMenuLayout;  // Initialize the layout reference
    }

    /**
     * Starts the server to listen for incoming messages on the specified port.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);

        // Continuously listen for incoming connections
        while (running) {
            try {
                Socket socket = serverSocket.accept();  // Accept incoming connection
                new Thread(new ClientHandler(socket, mainMenuLayout)).start();  // Handle each client in a new thread
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Call this method to stop the server
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server stopped.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the communication with an individual client (peer) in a separate thread.
     */
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private BorderPane mainMenuLayout;  // Reference to the main layout for UI updates

        public ClientHandler(Socket socket, BorderPane mainMenuLayout) {
            this.socket = socket;
            this.mainMenuLayout = mainMenuLayout;  // Store layout reference for UI update
        }

        @Override
        public void run() {
            try {
                // Read the message sent by the peer
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message = reader.readLine();

                // Update the JavaFX UI on the JavaFX Application Thread
                Platform.runLater(() -> {
                    // Create a new label with the received message
                    Label messageLabel = new Label("Received message: " + message);

                    // Set this label to the center of the mainMenuLayout
                    StackPane messagePane = new StackPane(messageLabel);
                    mainMenuLayout.setCenter(messagePane);  // Update the center with the received message
                });

                socket.close();  // Close the connection after the message is received
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
