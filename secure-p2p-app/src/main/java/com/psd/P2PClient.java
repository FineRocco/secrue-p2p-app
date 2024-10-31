package com.psd;

import com.psd.entities.User;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

public class P2PClient {

    private final SSLSocketFactory sslSocketFactory;
    private SSLSocket clientSocket;

    /**
     * Initializes the P2PClient by setting up an SSL context, creating an SSLSocketFactory, and sending the user data to the central server.
     *
     * @param user The User object to be serialized and sent to the central server.
     */
    public P2PClient(User user) {
        try {
            // Set up SSL context for secure communication
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance("JKS");

            // Load the keystore from file
            try (InputStream keyStoreStream = new FileInputStream("keystore.jks")) {
                keyStore.load(keyStoreStream, "psd2024".toCharArray());
            }

            // Initialize KeyManager with keystore
            keyManagerFactory.init(keyStore, "psd2024".toCharArray());

            // Load and initialize truststore
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream trustStoreStream = new FileInputStream("truststore.jks")) {
                trustStore.load(trustStoreStream, "psd2024".toCharArray());
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // Configure SSLContext with key and trust managers
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            // Create SSLSocketFactory from SSLContext
            sslSocketFactory = sslContext.getSocketFactory();

            // Serialize the user object and send it to the central server
            List<byte[]> serializedUsers = new ArrayList<>();
            serializedUsers.add(serializeUser(user));
            sendUserToCentralServer(serializedUsers);

        } catch (Exception e) {
            System.out.println("Error initializing P2PClient: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize P2PClient", e);
        }
    }

    /**
     * Serializes a User object to a byte array.
     *
     * @param user The User object to be serialized.
     * @return A byte array representing the serialized user object.
     */
    private byte[] serializeUser(User user) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(user); // Serialize user object
            return byteOut.toByteArray();
        } catch (IOException e) {
            System.out.println("Error serializing user: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to serialize user", e);
        }
    }

    /**
     * Sends a list of serialized user data to the central server.
     *
     * @param users List of serialized user objects in byte array format.
     */
    public void sendUserToCentralServer(List<byte[]> users) {
        try {
            // Connect to central server over SSL
            clientSocket = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getLocalHost(), 8888);

            // Set up output stream for sending data
            DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());

            // Send the number of user objects
            dataOut.writeInt(users.size());

            // Send each user object
            for (byte[] user : users) {
                dataOut.writeInt(user.length); // Send length of user data
                dataOut.write(user); // Send user data
            }

            dataOut.flush();

        } catch (IOException e) {
            System.out.println("Error connecting to central server: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to central server", e);
        }
    }

    /**
     * Sends a message to a specified user by connecting to their IP and port over SSL.
     *
     * @param receiver The User object representing the message receiver.
     * @param message  The message to be sent as a byte array.
     * @throws IOException if an I/O error occurs during message sending.
     */
    public void sendMessage(User receiver, byte[] message) {
        try {
            // Connect to the receiver's IP and port over SSL
            clientSocket = (SSLSocket) sslSocketFactory.createSocket(receiver.getIpAddress(), receiver.getPort());

            // Initialize output stream for sending message
            DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());
            dataOut.writeInt(message.length); // Send message length
            dataOut.write(message); // Send message data
            dataOut.flush();
        } catch (IOException e) {
            System.out.println("Error connecting to receiver: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to receiver", e);
        }
    }

}
