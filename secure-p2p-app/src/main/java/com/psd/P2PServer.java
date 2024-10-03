package com.psd;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Represents a P2P server that listens for incoming messages from peers.
 * Each user has their own P2PServer running, which listens on the specified port.
 */
public class P2PServer {

    private int port;  // The port on which the server listens
    private SSLServerSocket serverSocket;
    private volatile boolean running = true;
    private BorderPane mainMenuLayout;  // Reference to the main layout in the JavaFX UI

    public P2PServer(int port, BorderPane mainMenuLayout) {
        this.port = port;
        this.mainMenuLayout = mainMenuLayout;  // Initialize the layout reference
    }

    /**
     * Starts the server to listen for incoming messages on the specified port.
     * @throws KeyManagementException 
     * @throws NoSuchAlgorithmException 
     * @throws KeyStoreException 
     * @throws UnrecoverableKeyException 
     * @throws CertificateException 
     */
    public void start() {
        try {
            // Setup SSL context with the keystore and truststore
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");

            // Load the keystore 
            try (InputStream keyStoreStream = new FileInputStream("keystore.jks")) {
                ks.load(keyStoreStream, "psd2024".toCharArray());  // Use your keystore password
            }

            kmf.init(ks, "psd2024".toCharArray());  // Initialize KeyManager with keystore password

            // Load the truststore
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream trustStoreStream = new FileInputStream("truststore.jks")) {
                trustStore.load(trustStoreStream, "psd2024".toCharArray());
            }

            // Initialize TrustManagerFactory with the truststore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // Initialize the SSLContext with both key managers and trust managers
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // Create SSLServerSocket
            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            serverSocket = (SSLServerSocket) ssf.createServerSocket(port);

            // Continuously listen for incoming connections
            while (running) {
                try {
                    SSLSocket socket = (SSLSocket) serverSocket.accept();  // Accept incoming connection
                    new Thread(new ClientHandler(socket, mainMenuLayout)).start();  // Handle each client in a new thread
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | UnrecoverableKeyException | CertificateException | IOException e) {
            e.printStackTrace();  // Handle SSL-related exceptions here
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
        private SSLSocket socket;
        private BorderPane mainMenuLayout;  // Reference to the main layout for UI updates

        public ClientHandler(SSLSocket socket, BorderPane mainMenuLayout) {
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
