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
import com.psd.services.EncriptionService;
import com.psd.services.SerializationService;
import com.psd.storage.AWS3Storage;
import com.psd.storage.AzureBlobStorage;
import com.psd.storage.FirebaseStorage;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import javax.crypto.SecretKey;
import javax.net.ssl.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.InetAddress;
import java.security.Security;
import java.util.List;

/**
 * Represents a P2P server that listens for incoming messages from peers.
 * Each user has their own {@code P2PServer} instance listening on a specified port.
 */
public class P2PServer {

    private static final Object userAuxLock = new Object();
    private static User userAux;
    private final User user;
    private final int port; // The port on which the server listens
    private SSLServerSocket serverSocket;
    private volatile boolean running = true; // Will be modified by different threads
    private BorderPane mainMenuLayout; // Reference to the main layout in the JavaFX UI
    private P2PClient client;
    private SSLServerSocketFactory sslServerSocketFactory;
    private static AWS3Storage aws3Storage = AWS3Storage.getInstance();
    private static FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private static AzureBlobStorage azureBlobStorage = AzureBlobStorage.getInstance();

        
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
                P2PServer.firebaseStorage = new FirebaseStorage();
                this.client = new P2PClient(user); // Initialize client
            new Thread(this::start).start(); // Start server on a new thread
        }
    
        /**
         * Starts the P2P server, initializing SSL settings and listening for incoming connections.
         */
        public void start() {
            try {
                EncriptionService.initializeServerKeys(user.getUserID());
                sslServerSocketFactory = EncriptionService.initializeServerSSLContext(user.getUserID());
                if (sslServerSocketFactory == null) {
                    System.out.println("Failed to create SSL peer server socket factory.");
                    return;
                }
    
                serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port, 50, InetAddress.getByName(user.getIpAddress()));
                System.out.println("P2PServer: Created socket with this data: " + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort());
    
                System.out.printf("P2PServer started at %s:%d%n", serverSocket.getInetAddress().getHostAddress(), port);
    
                // Listen for incoming connections
                while (running) {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket, mainMenuLayout)).start(); // Handle each client on a new thread
                }
            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
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
                sslServerSocketFactory = EncriptionService.initializeServerSSLContext(user.getUserID());

                // Ensure receiver details are up-to-date if missing
                if (receiver.getIpAddress() == null || receiver.getPort() == 0) {
                    client.sendUserToCentralServer(List.of(SerializationService.serialize(sender), SerializationService.serialize(receiver)));
                    synchronized (userAuxLock) {
                        userAuxLock.wait(); // Wait for receiver details update
                        receiver = userAux;
                    }
                }
                User finalReceiver = receiver;

                // Create the unique conversation ID based on sender and receiver
                String conversationId = Conversation.createConversation(sender, finalReceiver).getConversationId();

                // Attempt to load the conversation from AWS S3
                Conversation conversation = aws3Storage.loadConversation(conversationId);
                
                // If the conversation is not found in AWS S3, try loading it from Firebase
                if (conversation == null) {
                    conversation = firebaseStorage.loadConversation(conversationId);
                    System.out.println("Loaded conversation from Firebase.");
                }

                // If the conversation is not found in firebase, try loading it from AzureBlob
                if (conversation == null) {
                    conversation = azureBlobStorage.loadConversation(conversationId);
                    System.out.println("Loaded conversation from AzureBlob.");
                }
                
                // If the conversation is still not found, create a new one
                if (conversation == null) {
                    conversation = Conversation.createConversation(sender, finalReceiver);
                    System.out.println("Created a new conversation with ID: " + conversationId);
                }

                // Add message to conversation
                conversation.addMessage(message);

                // Save updated conversation to both AWS S3, Firebase and AzureBlob
                System.out.println("Saving conversation to AWS S3.");
                aws3Storage.saveConversation(conversationId, conversation);
                System.out.println("Saved conversation to Firebase.");
                firebaseStorage.saveConversation(conversationId, conversation);
                System.out.println("Saved conversation to Azure Blob.");
                azureBlobStorage.saveConversation(conversationId, conversation);

                // Send message to the receiver
                client.sendMessage(finalReceiver, SerializationService.serialize(message));
                return true;
            } catch (Exception e) {
                System.err.println("Failed to send message: " + e.getMessage());
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
                sslServerSocketFactory = EncriptionService.initializeServerSSLContext(user.getUserID());

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
                System.out.printf("Received message from %s:%d%n", message.getSender().getIpAddress(), message.getSender().getPort());

                // Create the unique conversation ID based on sender and receiver
                String conversationId = Conversation.createConversation(message.getSender(), message.getReceiver()).getConversationId();

                try {
                    // Attempt to load the conversation from AWS S3
                    Conversation conversation = aws3Storage.loadConversation(conversationId);

                    // If the conversation is not found in AWS S3, try loading it from Firebase
                    if (conversation == null) {
                        conversation = firebaseStorage.loadConversation(conversationId);
                        System.out.println("Loaded conversation from Firebase.");
                    }

                    // If the conversation is not found in firebase, try loading it from AzureBlob
                    if (conversation == null) {
                        conversation = azureBlobStorage.loadConversation(conversationId);
                        System.out.println("Loaded conversation from AzureBlob.");
                    }

                    // If the conversation is still not found, create a new one
                    if (conversation == null) {
                        conversation = Conversation.createConversation(message.getSender(), message.getReceiver());
                        System.out.println("Created a new conversation with ID: " + conversationId);
                    }

                    // Save updated conversation to both AWS S3, Firebase and AzureBlob
                    aws3Storage.saveConversation(conversationId, conversation);
                    firebaseStorage.saveConversation(conversationId, conversation);
                    azureBlobStorage.saveConversation(conversationId, conversation);

                    // Update the UI to display the received message
                    Platform.runLater(() -> {
                        Label messageLabel = new Label("Received message from: " + message.getSender().getUserID());
                        mainMenuLayout.setCenter(new StackPane(messageLabel)); // Display message in UI
                    });
                } catch (IOException e) {
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
