/**
 * The {@code P2PServer} class represents a peer-to-peer (P2P) server in a secure messaging
 * application. It enables secure communication between users, managing conversations and 
 * handling incoming messages from peers. Each user has an instance of {@code P2PServer} that
 * listens on a designated port for incoming connections, allowing direct peer-to-peer messaging.
 *
 * <p>Main functionalities include:
 * <ul>
 *   <li>Starting an SSL/TLS-enabled server to accept incoming connections</li>
 *   <li>Sending direct messages between peers and updating conversations</li>
 *   <li>Handling client connections using a {@link ClientHandler} to manage message and user data</li>
 *   <li>Maintaining active conversations between users</li>
 * </ul>
 *
 * <p>Dependencies: This class relies on {@code EncriptionService} for SSL/TLS setup and 
 * {@code SerializationService} for data serialization. It also updates the JavaFX UI to display 
 * incoming messages.
 */
package com.psd;

import com.amazonaws.client.ClientHandler;
import com.psd.entities.Conversation;
import com.psd.entities.Group;
import com.psd.entities.Message;
import com.psd.entities.MessageGroup;
import com.psd.entities.User;
import com.psd.services.EncryptionService;
import com.psd.services.SSLService;
import com.psd.services.SecretSharingService;
import com.psd.services.SerializationService;
import com.psd.storage.AWS3Storage;
import com.psd.storage.AzureBlobStorage;
import com.psd.storage.FirebaseStorage;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import javax.net.ssl.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.Map;

/**
 * Represents a P2P server that listens for incoming messages from peers.
 * Each user has their own {@code P2PServer} instance listening on a specified port.
 */
public class P2PServer {

    private static final Object userAuxLock = new Object();
    private static User userAux;
    private final User user;
    private final int port; // The port on which the server listens
    public static String secretKeyCloud; // The secret key
    private SSLServerSocket serverSocket;
    private volatile boolean running = true; // Will be modified by different threads
    private BorderPane mainMenuLayout; // Reference to the main layout in the JavaFX UI
    private P2PClient client;
    private SSLServerSocketFactory sslServerSocketFactory;
    private static AWS3Storage aws3Storage = AWS3Storage.getInstance();
    private static FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private static AzureBlobStorage azureBlobStorage = AzureBlobStorage.getInstance();
    // Generate and split the key
    Map<Integer, String> shares;

        
        static {
            // Register the Bouncy Castle provider
            Security.addProvider(new BouncyCastleProvider());
        }
        
        /**
         * Constructs a {@code P2PServer} for a specified user, with a reference to the JavaFX main menu layout.
         * This constructor starts the server in a new thread.
         *
         * @param user The user for whom this server is created.
         * @param mainMenuLayout The main menu layout in JavaFX UI.
         */
        public P2PServer(User user, BorderPane mainMenuLayout) {
            this.user = user;
            this.port = user.getPort();
            this.mainMenuLayout = mainMenuLayout;
            this.shares = SecretSharingService.generateAndSplitKey();
            this.client = new P2PClient(user); // Initialize client
        new Thread(this::start).start(); // Start server on a new thread
        }

        
    
        /**
         * Starts the P2P server, initializing SSL settings and listening for incoming connections and sharing the secret between clouds
         */
        public void start() {
            try {
                SSLService.initializeServerKeys(user.getUserID());
                sslServerSocketFactory = SSLService.initializeServerSSLContext(user.getUserID());
                if (sslServerSocketFactory == null) {
                    System.out.println("Failed to create SSL peer server socket factory.");
                    return;
                }
    
                serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port, 50, InetAddress.getByName(user.getIpAddress()));
                System.out.println("P2PServer: Created socket with this data: " + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort());
    
                System.out.printf("P2PServer started at %s:%d%n", serverSocket.getInetAddress().getHostAddress(), port);

                ensureKeySharesExist(user.getUserID()); // Ensure key shares exist in the clouds or creates it
    
                // Listen for incoming connections
                while (running) {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket, mainMenuLayout)).start(); // Handle each client on a new thread
                }
            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
            }
        }

        public static void saveConversationClouds(String conversationId, String encryptedConversation, String userId){
            // Save updated conversation to both AWS S3, Firebase and AzureBlob
            try {
                System.out.println("Saving conversation to AWS S3.");
                aws3Storage.saveEncryptedConversation(conversationId, encryptedConversation, userId);
                System.out.println("Saved conversation to Firebase.");
                firebaseStorage.saveEncryptedConversation(conversationId, encryptedConversation, userId);
                System.out.println("Saved conversation to Azure Blob.");
                azureBlobStorage.saveEncryptedConversation(conversationId, encryptedConversation, userId);
            } catch (IOException e) {
                System.err.println("Error saving conversation to cloud: " + e.getMessage());
                e.printStackTrace();
            }
        }

        public static String loadConversationClouds(String conversationId, String userId){
            try {
                // Attempt to load the conversation from AWS S3
                String conversation = aws3Storage.loadEncryptedConversation(conversationId, userId);

                // If the conversation is not found in AWS S3, try loading it from Firebase
                if (conversation == null) {
                    conversation = firebaseStorage.loadEncryptedConversation(conversationId, userId);
                    System.out.println("Loaded conversation from Firebase.");
                }

                // If the conversation is not found in firebase, try loading it from AzureBlob
                if (conversation == null) {
                    conversation = azureBlobStorage.loadEncryptedConversation(conversationId, userId);
                    System.out.println("Loaded conversation from AzureBlob.");
                }
                return conversation;

            } catch (IOException e) {
                System.err.println("Error loading conversation from cloud: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        
        /**
         * Ensures that key shares exist in the clouds. If not, generates and distributes shares.
         *
         * @param userId The user ID to associate the shares with.
         */
        public void ensureKeySharesExist(String userId) {
            try {
                // Check if shares exist in the clouds
                boolean awsShareExists = aws3Storage.checkShareExists(userId, "share1");
                boolean firebaseShareExists = firebaseStorage.checkShareExists(userId, "share2");
                boolean azureShareExists = azureBlobStorage.checkShareExists(userId, "share3");

                // If all shares exist, do nothing
                if (awsShareExists && firebaseShareExists && azureShareExists) {
                    System.out.println("All shares exist. Reconstructing the key...");

                    String share1 = aws3Storage.loadKeyShare(userId, "share1");
                    String share2 = firebaseStorage.loadKeyShare(userId, "share2");
                    String share3 = azureBlobStorage.loadKeyShare(userId, "share3");
        
                    // Combine the retrieved shares into a map
                    Map<Integer, String> retrievedShares = Map.of(
                        1, share1,
                        2, share2,
                        3, share3
                    );
        
                    // Reconstruct the key
                    P2PServer.secretKeyCloud = SecretSharingService.reconstructKey(retrievedShares);
                    System.out.println("Reconstructed Secret Key: " + P2PServer.secretKeyCloud);
                    return;
                }

                // Generate and split a new key if shares are missing
                System.out.println("Generating new key and shares...");
                Map<Integer, String> shares = SecretSharingService.generateAndSplitKey();

                // Extract the original key (used for your secretKey field)
                P2PServer.secretKeyCloud = SecretSharingService.reconstructKey(shares);
                System.out.println("Generated Secret Key: " + P2PServer.secretKeyCloud);

                // Distribute shares to the clouds
                if (!awsShareExists) {
                    aws3Storage.saveKeyShare(userId, "share1", shares.get(1));
                    System.out.println("Saved share1 to AWS.");
                }

                if (!firebaseShareExists) {
                    firebaseStorage.saveKeyShare(userId, "share2", shares.get(2));
                    System.out.println("Saved share2 to Firebase.");
                }

                if (!azureShareExists) {
                    azureBlobStorage.saveKeyShare(userId, "share3", shares.get(3));
                    System.out.println("Saved share3 to Azure.");
                }
            } catch (Exception e) {
                System.err.println("Error ensuring key shares: " + e.getMessage());
            }
        }
    
        /**
         * Sends a direct message from the sender to the receiver. If the receiver's information is missing,
         * it requests the details from the central server before sending.
         *
         * @param message The {@link Message} to be sent.
         * @param sender The {@link User} sending the message.
         * @param receiver The {@link User} receiving the message.
         * @return {@code true} if the message is sent successfully; {@code false} otherwise.
         */
        public boolean sendDirectMessage(Message message, User sender, User receiver) {
            try {
                // Initialize SSL context
                try {
                    sslServerSocketFactory = SSLService.initializeServerSSLContext(user.getUserID());
                } catch (Exception e) {
                    System.err.println("Error initializing SSL context: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }

                // Ensure receiver details are up-to-date if missing
                if (receiver.getIpAddress() == null || receiver.getPort() == 0) {
                    try {
                        client.sendUserToCentralServer(List.of(SerializationService.serialize(sender), SerializationService.serialize(receiver)));
                        synchronized (userAuxLock) {
                            userAuxLock.wait(); // Wait for receiver details update
                            receiver = userAux;
                        }
                    } catch (Exception e) {
                        System.err.println("Error updating receiver details: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }

                User finalReceiver = receiver;

                // Create the unique conversation ID based on sender and receiver
                String conversationId;
                try {
                    conversationId = Conversation.createConversation(sender, finalReceiver).getConversationId();
                    System.out.println("Generated conversation ID: " + conversationId);
                } catch (Exception e) {
                    System.err.println("Error generating conversation ID: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }

                // Attempt to load the conversation from cloud storage
                String encryptedConversation;
                try {
                    encryptedConversation = loadConversationClouds(conversationId, sender.getUserID());
                    System.out.println("Loaded encrypted conversation: " + encryptedConversation);
                } catch (Exception e) {
                    System.err.println("Error loading conversation from cloud: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }

                Conversation conversation;

                try {
                    if (encryptedConversation == null) {
                        // If no conversation exists, create a new one
                        conversation = Conversation.createConversation(sender, finalReceiver);
                        System.out.println("Created a new conversation with ID: " + conversationId);

                        // Add message to the conversation
                        conversation.addMessage(message);

                        // Encrypt the new conversation
                        String newEncryptedConversation = EncryptionService.encryptObject(secretKeyCloud, conversation);

                        // Save the encrypted conversation to the clouds
                        saveConversationClouds(conversationId, newEncryptedConversation, sender.getUserID());
                    } else {
                        // Decrypt the existing conversation
                        System.out.println("Decrypting conversation with key: " + secretKeyCloud);
                        conversation = (Conversation) EncryptionService.decryptObject(secretKeyCloud, encryptedConversation);
                        System.out.println("Decrypted conversation with ID: " + conversationId);

                        // Add message to the conversation
                        conversation.addMessage(message);

                        // Encrypt the updated conversation
                        String newEncryptedConversation = EncryptionService.encryptObject(secretKeyCloud, conversation);

                        // Save the updated conversation to the clouds
                        saveConversationClouds(conversationId, newEncryptedConversation, sender.getUserID());
                    }
                } catch (Exception e) {
                    System.err.println("Error handling conversation encryption/decryption: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }

                // Send the message to the receiver
                try {
                    client.sendMessage(finalReceiver, SerializationService.serialize(message));
                    System.out.println("Message sent to receiver: " + finalReceiver.getUserID());
                } catch (Exception e) {
                    System.err.println("Error sending message to receiver: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }

                return true;

            } catch (Exception e) {
                System.err.println("Failed to send message: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        
        public void sendInterestsToCentralServer(User user){
            client.sendInterestsToCentralServer(SerializationService.serialize(user));
        }
        
        /**
         * Sends a group message from the sender to all members of a specified group.
         * If any group member's information is missing, it requests the details from the central server before sending.
         *
         * @param message The {@link MessageGroup} to be sent.
         * @param sender The {@link User} sending the message.
         * @param group The {@link Group} to which the message is being sent.
         * @return {@code true} if the message is sent successfully to all members; {@code false} otherwise.
         */
        public boolean sendMessageGroup(MessageGroup message, User sender, Group group) {
            boolean allMessagesSent = true;

            try {
                sslServerSocketFactory = SSLService.initializeServerSSLContext(user.getUserID());

                // Add the message to the group's messages map
                group.addMessage(message);


                // Save the updated group to all storage backends
                System.out.println("Saving group to AWS S3.");
                aws3Storage.saveGroup(group.getGroupID(), group);
                System.out.println("Saved group to Firebase.");
                firebaseStorage.saveGroup(group.getGroupID(), group);
                System.out.println("Saved group to Azure Blob.");
                azureBlobStorage.saveGroup(group.getGroupID(), group);

                // Send the message to each group member
                for (User member : group.getMembers()) {
                    // Skip the sender as they don't need to receive their own message
                    if (member.equals(sender)) {
                        continue;
                    }

                    // Ensure member details are up-to-date if missing
                    if (member.getIpAddress() == null || member.getPort() == 0) {
                        client.sendUserToCentralServer(List.of(SerializationService.serialize(sender), SerializationService.serialize(member)));
                        synchronized (userAuxLock) {
                            userAuxLock.wait(); // Wait for member details update
                            member = userAux;
                        }
                    }

                    // Send the message to the group member
                    try {
                        client.sendMessage(member, SerializationService.serialize(message));
                    } catch (Exception e) {
                        System.err.println("Failed to send message to group member: " + member.getUserID());
                        allMessagesSent = false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to send group message: " + e.getMessage());
                return false;
            }

            return allMessagesSent;
        }
        
            
        /**
         * Handles communication with an individual client in a separate thread.
         * Each client interaction is managed through an instance of this class.
         */
        private static class ClientHandler implements Runnable {
            private final SSLSocket socket;
            private final BorderPane mainMenuLayout;
    
            /**
             * Constructs a {@code ClientHandler} with the specified SSL socket and main menu layout.
             *
             * @param socket The {@link SSLSocket} connected to the client.
             * @param mainMenuLayout The main layout in JavaFX UI where messages are displayed.
             */
            public ClientHandler(SSLSocket socket, BorderPane mainMenuLayout) {
                this.socket = socket;
                this.mainMenuLayout = mainMenuLayout;
            }
            
            @Override
            public void run() {
                try (DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {
                    int messageLength = dataIn.readInt();
                    byte[] messageBytes = new byte[messageLength];
                    dataIn.readFully(messageBytes);
    
                    // Handle either a Message or a User object
                    Object receivedObject = SerializationService.deserialize(messageBytes);
                    if (receivedObject instanceof Message) {
                        handleMessage((Message) receivedObject);
                    } else if (receivedObject instanceof User) {
                        updateUserAux((User) receivedObject);
                    }   else if (receivedObject instanceof User) {
                        handleMessageGroup((MessageGroup) receivedObject);
                    }
                } catch (Exception e) {
                    System.err.println("ClientHandler error: " + e.getMessage());
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.err.println("Failed to close socket: " + e.getMessage());
                    }
                }
            }
            
            /**
             * Updates the userAux object with new user details, notifying any waiting threads.
             *
             * @param user The {@link User} with updated details.
             */
            private void updateUserAux(User user) {
                synchronized (userAuxLock) {
                    userAux = user;
                    userAuxLock.notifyAll(); // Notify waiting threads of the update
                }
            }
            
            /**
             * Processes a received message, adding it to the appropriate conversation and updating the UI.
             *
             * @param message The {@link Message} received from a peer.
             */
            private void handleMessage(Message message) {
                User sender = message.getSender();
                User receiver = message.getReceiver();
                System.out.printf("Received message from %s:%d%n", sender.getIpAddress(), sender.getPort());

                try {
                    // Create the unique conversation ID based on sender and receiver
                    String conversationId = Conversation.createConversation(sender, receiver).getConversationId();

                    // Attempt to load the conversation from AWS S3
                    String encryptedConversation = loadConversationClouds(conversationId, receiver.getUserID());
                    
                    // If the conversation is still not found, create a new one
                    if (encryptedConversation == null) {
                        Conversation conversation = Conversation.createConversation(sender, receiver);
                        System.out.println("Created a new conversation with ID: " + conversationId);

                        // Add message to conversation
                        conversation.addMessage(message);

                        // Encrypt the conversation using the shared secret key
                        String newEncryptedConversation = EncryptionService.encryptObject(secretKeyCloud, conversation);

                        // Save updated conversation to both AWS S3, Firebase and AzureBlob
                        saveConversationClouds(conversationId, newEncryptedConversation, receiver.getUserID());
                    } else {
                        // Decrypt the conversation using the shared secret key
                        Conversation conversation = (Conversation) EncryptionService.decryptObject(secretKeyCloud, encryptedConversation);

                        // Add message to conversation
                        conversation.addMessage(message);

                        // Encrypt the conversation using the shared secret key
                        String newEncryptedConversation = EncryptionService.encryptObject(secretKeyCloud, conversation);

                        // Save updated conversation to both AWS S3, Firebase and AzureBlob
                        saveConversationClouds(conversationId, newEncryptedConversation, receiver.getUserID());
                    }

                    // Update the UI to display the received message
                    Platform.runLater(() -> {
                        Label messageLabel = new Label("Received message from: " + message.getSender().getUserID());
                        mainMenuLayout.setCenter(new StackPane(messageLabel)); // Display message in UI
                    });
                } catch (IOException |GeneralSecurityException | ClassNotFoundException e) {
                    System.err.println("Error handling message: " + e.getMessage());
                }
            }

            /**
             * Processes a received group message, adding it to the appropriate group conversation and updating the UI.
             *
             * @param messageGroup The {@link MessageGroup} received from a peer.
             * @param groupId The unique ID of the group to which the message belongs.
             */
            private void handleMessageGroup(MessageGroup messageGroup) {
                System.out.printf("Received group message from %s:%d for group %s%n", 
                                messageGroup.getSender().getIpAddress(), messageGroup.getSender().getPort(), messageGroup.getGroupId());

                try {
                    // Attempt to load the group conversation from AWS S3
                    Group groupConversation = aws3Storage.loadGroup(messageGroup.getGroupId());

                    // If the group conversation is not found in AWS S3, try loading it from Firebase
                    if (groupConversation == null) {
                        groupConversation = firebaseStorage.loadGroup(messageGroup.getGroupId());
                        System.out.println("Loaded group conversation from Firebase.");
                    }

                    // If the group conversation is not found in Firebase, try loading it from Azure Blob
                    if (groupConversation == null) {
                        groupConversation = azureBlobStorage.loadGroup(messageGroup.getGroupId());
                        System.out.println("Loaded group conversation from Azure Blob.");
                    }

                    // If the group conversation is still not found, print an error and exit
                    if (groupConversation == null) {
                        System.err.println("Group conversation with ID " + messageGroup.getGroupId() + " not found in any storage.");
                        return;
                    }

                    // Save updated group conversation to all storage backends
                    aws3Storage.saveGroup(messageGroup.getGroupId(), groupConversation);
                    firebaseStorage.saveGroup(messageGroup.getGroupId(), groupConversation);
                    azureBlobStorage.saveGroup(messageGroup.getGroupId(), groupConversation);

                    // Use a final variable for the group topic to pass it into the lambda
                    final String groupTopic = groupConversation.getGroupID();

                    // Update the UI to display the received group message
                    Platform.runLater(() -> {
                        Label messageLabel = new Label("Received group message from: " + messageGroup.getSender().getUserID() + 
                                                    " in group: " + groupTopic);
                        mainMenuLayout.setCenter(new StackPane(messageLabel)); // Display message in UI
                    });
                } catch (IOException e) {
                    System.err.println("Error handling group message: " + e.getMessage());
                }
            }

    }
}
