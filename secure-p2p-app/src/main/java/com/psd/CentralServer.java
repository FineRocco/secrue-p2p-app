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

import com.psd.entities.Group;
import com.psd.entities.User;
import com.psd.services.SSLService;
import com.psd.services.SerializationService;
import com.psd.storage.AWS3Storage;
import com.psd.storage.AzureBlobStorage;
import com.psd.storage.FirebaseStorage;

import javax.crypto.SecretKey;
import javax.net.ssl.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        initializeSSLGroups();
        sslServerSocketFactory = SSLService.initializeServerSSLContext("server");
        if (sslServerSocketFactory == null) {
            System.out.println("Failed to create SSL server socket factory.");
            return;
        }

        startServer(sslServerSocketFactory);
    }

    /**
     * Initializes predefined groups with their topics, trustStores, and encryption keys.
     */
    private static void initializeSSLGroups() {
        List<String> groupIDs = Arrays.asList("football", "ufc", "basketball");

        for (String groupID : groupIDs) {
            try {
                List<Group> groups = new ArrayList<>();
                try {
                    // Attempt to retrieve all groups for the current user from AWS S3
                    groups = aws3Storage.getAllGroups();
                    System.out.println("Successfully retrieved groups from AWS S3.");
                } catch (Exception awsException) {
                    System.err.println("Error retrieving groups from AWS S3: " + awsException.getMessage());
                    
                    // Attempt to retrieve from Firebase if AWS S3 fails
                    System.out.println("Attempting to retrieve groups from Firebase instead...");
                    try {
                        groups = firebaseStorage.getAllGroups();
                        System.out.println("Successfully retrieved groups from Firebase.");
                    } catch (Exception firebaseException) {
                        System.err.println("Error retrieving groups from Firebase: " + firebaseException.getMessage());
                        
                        // If both AWS S3 and Firebase retrieval fail, attempt Azure Blob Storage
                        System.out.println("Attempting to retrieve groups from Azure Blob Storage instead...");
                        try {
                            groups = azureBlobStorage.getAllGroups();
                            System.out.println("Successfully retrieved groups from Azure Blob Storage.");
                        } catch (Exception azureException) {
                            System.err.println("Error retrieving groups from Azure Blob Storage: " + azureException.getMessage());
                        }
                    }
                }
                List<Group> Finalgroups = groups;
                for (Group group : Finalgroups) {
                    if (group.getGroupID().equals(groupID)) {
                        System.out.println("Group " + groupID + " already exists.");
                        continue;
                    }else {
                    // Create a new group instance with the topic
                    Group newGroup = new Group(groupID, new ArrayList<>());

                    // Save the group to all storages (without the SecretKey)
                    aws3Storage.saveGroup(groupID, newGroup);
                    firebaseStorage.saveGroup(groupID, newGroup);
                    azureBlobStorage.saveGroup(groupID, newGroup);
                    }
             }
            } catch (IOException e) {
                System.err.println("Error initializing group " + groupID + ": " + e.getMessage());
            }
        }
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
    public static synchronized void registerUser(User user) {
        userRegistry.put(user.getUserID(), user);
        System.out.println("User registered: " + user.getUserID());
    }

    /**
     * Retrieves a user from the registry based on the user ID.
     *
     * @param userId The unique identifier of the user.
     * @return The {@link User} object if found, or {@code null} if no matching user exists.
     */
    public static synchronized User getUser(String userId) {
        return userRegistry.get(userId);
    }

    /**
     * Sends a {@link User} record to another user if available; otherwise, sends a '0' to indicate
     * that the requested user does not exist.
     *
     * @param toSend The {@link User} to whom the information is sent.
     * @param requestedUser The {@link User} whose information is requested. If {@code null}, a '0' is sent.
     */
    public static void sendUser(User toSend, User requestedUser) {
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
            try (DataInputStream dataIn = new DataInputStream(socket.getInputStream());
                 DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {

                int requestType = dataIn.readInt(); // Read request type

                switch (requestType) {
                    case 1: // User registration or retrieval
                        handleUserRequest(dataIn, dataOut);
                        break;

                    case 2: // User interest update with group members request
                        handleGroupUpdate(dataIn, dataOut);
                        break;

                    default:
                        System.out.println("Unknown request type: " + requestType);
                }

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
        private void handleUserRequest(DataInputStream dataIn, DataOutputStream dataOut) throws IOException {
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

        /**
         * Handles group updates by adding the received user as a member to all groups 
         * where the group ID matches an interest in the user's list of interests.
         */
        private void handleGroupUpdate(DataInputStream dataIn, DataOutputStream dataOut) throws IOException {

            // Read the serialized User object from the input
            int userLength = dataIn.readInt(); // Read the length of the serialized User object
            byte[] userBytes = new byte[userLength];
            dataIn.readFully(userBytes); // Read the User object bytes fully
            User user = (User) SerializationService.deserialize(userBytes); // Deserialize the User object

            System.out.println("Received group update request for user: " + user.getUserID());

            List<Group> groupsStorage = new ArrayList<>();
            try {
                // Attempt to retrieve all groups for the current user from AWS S3
                groupsStorage = aws3Storage.getAllGroups();
                System.out.println("Successfully retrieved groups from AWS S3.");
            } catch (Exception awsException) {
                System.err.println("Error retrieving groups from AWS S3: " + awsException.getMessage());
                
                // Attempt to retrieve from Firebase if AWS S3 fails
                System.out.println("Attempting to retrieve groups from Firebase instead...");
                try {
                    groupsStorage = firebaseStorage.getAllGroups();
                    System.out.println("Successfully retrieved groups from Firebase.");
                } catch (Exception firebaseException) {
                    System.err.println("Error retrieving groups from Firebase: " + firebaseException.getMessage());
                    
                    // If both AWS S3 and Firebase retrieval fail, attempt Azure Blob Storage
                    System.out.println("Attempting to retrieve groups from Azure Blob Storage instead...");
                    try {
                        groupsStorage = azureBlobStorage.getAllGroups();
                        System.out.println("Successfully retrieved groups from Azure Blob Storage.");
                    } catch (Exception azureException) {
                        System.err.println("Error retrieving groups from Azure Blob Storage: " + azureException.getMessage());
                    }
                }
            }

            // Iterate over the user's interests and add the user as a member to matching groups
            System.out.println("Interests from user after sending to CentralServer: " + user.getInterests());
            for (String interest : user.getInterests()) {

                for (Group group : groupsStorage) {
                    if (group.getGroupID().equals(interest)) {
                        // Check if the user is already a member; if not, add them
                        if (!group.getMembers().contains(user)) {
                            group.addMember(user);
                            System.out.println("Added " + user.getUserID() + " as a member to group: " + interest);

                            // Save the updated group to all storage backends (without the SecretKey)
                            aws3Storage.saveGroup(interest.toLowerCase(), group);
                            firebaseStorage.saveGroup(interest.toLowerCase(), group);
                            azureBlobStorage.saveGroup(interest.toLowerCase(), group);

                        }
                    }
                }
            }
        }
    
    }
}
