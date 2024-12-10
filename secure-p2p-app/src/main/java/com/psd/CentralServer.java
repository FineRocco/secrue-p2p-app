/**
 * The {@code CentralServer} class serves as the main server in a secure peer-to-peer application.
 * It uses SSL/TLS for secure communication and manages a registry of users, enabling the registration
 * of new users and the retrieval of registered user information. Communication is handled via 
 * SSL sockets to maintain privacy and authenticity in the peer-to-peer network.
 *
 * <p>Key functionalities include:
 * <ul>
 *   <li>Starting the server and accepting SSL connections from clients</li>
 *   <li>Registering new users</li>
 *   <li>Retrieving registered users</li>
 *   <li>Sending user data to requesting peers</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 *     CentralServer.registerUser(newUser);
 *     User retrievedUser = CentralServer.getUser("userId");
 * </pre>
 *
 * <p>Dependencies: This class requires {@code EncriptionService} for SSL/TLS setup and certificate management,
 * and {@code SerializationService} for serializing and deserializing user data.
 */
package com.psd;

import com.psd.entities.User;
import com.psd.services.SSLService;
import com.psd.services.SerializationService;
import com.psd.storage.AWS3Storage;
import com.psd.storage.AzureBlobStorage;
import com.psd.storage.FirebaseStorage;

import javax.net.ssl.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.security.Security;
import java.util.concurrent.ConcurrentHashMap;

public class CentralServer {
    private static final ConcurrentHashMap<String, User> userRegistry = new ConcurrentHashMap<>(); // Stores username and associated user info
    public static final int SERVER_PORT = 8888;
    private static volatile boolean isRunning = false;
    private static SSLSocketFactory sslSocketFactory;
    private static SSLServerSocketFactory sslServerSocketFactory;
    private static AWS3Storage aws3Storage = AWS3Storage.getInstance();
    private static FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private static AzureBlobStorage azureBlobStorage = AzureBlobStorage.getInstance();


    static {
        // Register the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        // Load SSL context
        SSLService.initializeServerKeys("server");
        SSLService.initializeServerTruststore();
        sslServerSocketFactory = SSLService.initializeServerSSLContext("server");
        if (sslServerSocketFactory == null) {
            System.out.println("Failed to create SSL server socket factory.");
            return;
        }

        startServer(sslServerSocketFactory);
    }

    /**
     * Starts the central server and accepts SSL client connections.
     * Each connection is handled in a new thread via a {@link ClientHandler}.
     *
     * @param sslServerSocketFactory The SSLServerSocketFactory for creating secure sockets.
     */
    private static void startServer(SSLServerSocketFactory sslServerSocketFactory) {
        new Thread(() -> {
            try (SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(SERVER_PORT, 50, InetAddress.getLocalHost())) {
                System.out.println("Central Server started on: " + InetAddress.getLocalHost().getHostAddress() + " : " + SERVER_PORT);

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

    /**
     * Registers a new user to the central server by storing the user information in the registry.
     *
     * @param user The {@link User} object representing the user to be registered.
     */
    private static synchronized void registerUser(User user) {
        userRegistry.put(user.getUserID(), user);
        System.out.println("User registered: " + user.getUserID());
    }



    /**
     * Retrieves a user from the registry based on the user ID.
     *
     * @param userId The unique identifier of the user.
     * @return The {@link User} object if found, or {@code null} if no matching user exists.
     */
    private static synchronized User getUser(String userId) {
        return userRegistry.get(userId);
    }

    /**
     * Sends a {@link User} record to another user if available; otherwise, sends a '0' to indicate
     * that the requested user does not exist.
     *
     * @param toSend The {@link User} to whom the information is sent.
     * @param requestedUser The {@link User} whose information is requested. If {@code null}, a '0' is sent.
     */
    private static void sendUser(User toSend, User requestedUser) {
        try {
            sslSocketFactory = SSLService.initializeSSLContext(requestedUser.getUserID());
            SSLSocket clientSocket = (SSLSocket) sslSocketFactory.createSocket(toSend.getIpAddress(), toSend.getPort());

            DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());

            if (requestedUser != null) {
                byte[] userBytes = SerializationService.serialize(requestedUser);

                dataOut.writeInt(userBytes.length); // Send the length of the serialized user data
                dataOut.write(userBytes); // Send the serialized user data

            } else {
                dataOut.writeInt(0); // '0' indicates no user data sent
            }
            dataOut.flush();
        } catch (IOException e) {
            System.out.println("Error sending user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles communication with an individual client in a separate thread.
     * Each client interaction is managed through an instance of this class.
     */
    private static class ClientHandler implements Runnable {
        private final SSLSocket socket;

        /**
         * Constructs a {@code ClientHandler} with the given SSL socket.
         *
         * @param socket The {@link SSLSocket} connected to the client.
         */
        public ClientHandler(SSLSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dataIn = new DataInputStream(socket.getInputStream());){

                int requestType = dataIn.readInt(); // Read request type

                handleUserRequest(dataIn);


            } catch (IOException e) {
                System.out.println("Client handler error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        }

        /**
         * Handles user registration or retrieval based on received data.
         */
        private void handleUserRequest(DataInputStream dataIn) throws IOException {
            int nrUsers = dataIn.readInt();
            List<User> users = new ArrayList<>(nrUsers);

            for (int i = 0; i < nrUsers; i++) {
                int messageLength = dataIn.readInt();
                byte[] messageBytes = new byte[messageLength];
                dataIn.readFully(messageBytes);
                users.add((User) SerializationService.deserialize(messageBytes));
            }

            if (nrUsers == 1) {
                registerUser(users.get(0));
            } else {
                sendUser(users.get(0), getUser(users.get(1).getUserID()));
            }
        }
    
    }
}
