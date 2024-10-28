package com.psd;

import com.psd.entities.User;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.concurrent.ConcurrentHashMap;

public class CentralServer {
    private static final ConcurrentHashMap<String, User> userRegistry = new ConcurrentHashMap<>(); // Stores username and associated user info
    private static volatile boolean isRunning = false;
    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) {
        // Load SSL context
        SSLServerSocketFactory sslServerSocketFactory = createSSLServerSocketFactory();
        if (sslServerSocketFactory == null) {
            System.out.println("Failed to create SSL server socket factory.");
            return;
        }

        new Thread(() -> {
            try (SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(SERVER_PORT, 50, InetAddress.getByName("localhost"))) {
                System.out.println("Central Server started on port: " + SERVER_PORT);
                isRunning = true;

                while (isRunning) {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start(); // Run each client handler in a new thread
                }
            } catch (IOException e) {
                System.out.println("Error starting server: " + e.getMessage());
            }
        }).start();
    }

    // Registers a new user to the central server
    public static synchronized void registerUser(User user) {
        userRegistry.put(user.getUserName(), user);
        System.out.println("User registered: " + user.getUserName());
    }

    // Fetches a user from the registry based on username
    public static synchronized User getUser(String username) {
        return userRegistry.get(username);
    }

    private static SSLServerSocketFactory createSSLServerSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");

            // Load the keystore
            try (InputStream keyStoreStream = new FileInputStream("serverkeystore.jks")) {
                ks.load(keyStoreStream, "centralServer".toCharArray());  // Use your keystore password
            }

            kmf.init(ks, "centralServer".toCharArray());  // Initialize KeyManager with keystore password

            // Load the truststore
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream trustStoreStream = new FileInputStream("servertruststore.jks")) {
                trustStore.load(trustStoreStream, "centralServer".toCharArray());
            }

            // Initialize TrustManagerFactory with the truststore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // Initialize the SSLContext with both key managers and trust managers
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return sslContext.getServerSocketFactory();
        } catch (Exception e) {
            System.out.println("Error creating SSL context: " + e.getMessage());
            return null;
        }
    }

    /**
     * Handles communication with an individual client in a separate thread.
     */
    private static class ClientHandler implements Runnable {
        private final SSLSocket socket;

        public ClientHandler(SSLSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream objectIn = new DataInputStream(socket.getInputStream());
                 DataOutputStream objectOut = new DataOutputStream(socket.getOutputStream())) {

                // Read the User object from the client
                //User receivedUser = (User) objectIn.readObject();

                // Determine if this is a registration or lookup request based on User's IP and port
                /* if (receivedUser.getIpAddress() == null && receivedUser.getPort() == 0) {
                    // This is a lookup request
                    User foundUser = getUser(receivedUser.getUserName());
                    objectOut.writeObject(foundUser); // Send the user details if found
                } else {*/
                // This is a registration request
                //registerUser(receivedUser); // Register the new user
                //objectOut.writeObject("Registration successful for " + receivedUser.getUserName());

                socket.close();
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());

            }
        }
    }
}
