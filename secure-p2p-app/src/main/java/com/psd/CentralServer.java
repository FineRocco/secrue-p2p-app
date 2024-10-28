package com.psd;

import com.psd.entities.Message;
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
            try (SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(SERVER_PORT, 50, InetAddress.getLocalHost())) {
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
    private static User deserializeUser(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(byteIn)) {
            return (User) in.readObject();
        }
    }
    private static SSLServerSocketFactory createSSLServerSocketFactory() {
        try {
            // Setup SSL context with the keystore and truststore
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");

            // Load the keystore (adjust the path to your keystore)
            try (InputStream keyStoreStream = new FileInputStream("serverkeystore.jks")) {
                ks.load(keyStoreStream, "centralServer".toCharArray());
            }

            // Initialize KeyManagerFactory with the keystore
            kmf.init(ks, "centralServer".toCharArray());

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
            try{
                DataInputStream dataIn = new DataInputStream(socket.getInputStream());

                // Read the length of the incoming message
                int messageLength = dataIn.readInt();

                // Initialize a byte array to hold the exact message size
                byte[] messageBytes = new byte[messageLength];

                // Read the message into the byte array
                int totalBytesRead = 0;
                while (totalBytesRead < messageLength) {
                    int bytesRead = dataIn.read(messageBytes, totalBytesRead, messageLength - totalBytesRead);
                    if (bytesRead == -1) break; // End of stream
                    totalBytesRead += bytesRead;
                }

                // Deserialize the message
                User user = deserializeUser(messageBytes);

                registerUser(user);
                System.out.println(userRegistry.size());
                socket.close();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error handling client: " + e.getMessage());

            }
        }
    }
}
