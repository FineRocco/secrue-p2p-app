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
import com.psd.entities.Message;
import com.psd.entities.User;
import com.psd.services.EncriptionService;
import com.psd.services.SerializationService;
import com.psd.storage.AWS3Storage;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javax.net.ssl.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Base64;
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
    private static AWS3Storage aws3Storage;
        
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
                P2PServer.aws3Storage = new AWS3Storage();
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
    
                this.client = new P2PClient(user); // Initialize client to send messages
    
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
                
                if (receiver.getIpAddress() == null || receiver.getPort() == 0) {
                    client.sendUserToCentralServer(List.of(SerializationService.serialize(sender), SerializationService.serialize(receiver)));
                    synchronized (userAuxLock) {
                        userAuxLock.wait(); // Wait for receiver details update
                        receiver = userAux;
                    }
                }
                User finalReceiver = receiver;
                String conversationKey = getConversationKey(sender, finalReceiver);
                
                // Load existing conversation from S3 or create new
                Conversation conversation = aws3Storage.loadConversation(conversationKey);
                if (conversation == null) {
                    conversation = new Conversation(sender, finalReceiver);
                }
        
                // Add message and save updated conversation to S3
                conversation.addMessage(message);
                aws3Storage.saveConversation(conversationKey, conversation);
        
                // Send message to the receiver
                client.sendMessage(finalReceiver, SerializationService.serialize(message));
                return true;
            } catch (Exception e) {
                System.err.println("Failed to send message: " + e.getMessage());
                return false;
            }
        }
        
        /**
         * Creates a unique key for each conversation between two users.
         *
         * @param user1 The first user in the conversation.
         * @param user2 The second user in the conversation.
         * @return A unique key representing the conversation.
         */
        private static String getConversationKey(User user1, User user2) {
            return (user1.getUserID().compareTo(user2.getUserID()) < 0)
                    ? user1.getUserID() + "-" + user2.getUserID()
                    : user2.getUserID() + "-" + user1.getUserID();
        }
    
        public List<Conversation> getAllConversations(User user) {
            List<Conversation> userConversations = new ArrayList<>();
            List<String> allKeys = aws3Storage.listAllConversationKeys(); // Get all stored keys

            System.out.println("Retrieved all conversation keys from S3: " + allKeys); // Log keys

            for (String key : allKeys) {
                try {
                    Conversation conversation = aws3Storage.loadConversation(key);
                    if (conversation != null) {
                        System.out.println("Loaded conversation for key: " + key + " with participants: " +
                                conversation.getParticipant1().getUserID() + " and " + conversation.getParticipant2().getUserID());

                        if (conversation.isParticipant(user)) {
                            System.out.println("Adding conversation for user: " + user.getUserID());
                            userConversations.add(conversation);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error loading conversation for key " + key + ": " + e.getMessage());
                }
            }
            return userConversations;
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
                
                String conversationKey = getConversationKey(message.getSender(), message.getReceiver());
                
                try {
                    // Load the conversation from S3 or create a new one if it doesn't exist
                    Conversation conversation = aws3Storage.loadConversation(conversationKey);
                    if (conversation == null) {
                        conversation = new Conversation(message.getSender(), message.getReceiver());
                    }
    
                    // Add the received message to the conversation and save it back to S3
                    conversation.addMessage(message);
                    aws3Storage.saveConversation(conversationKey, conversation);

                // Update the UI to display the received message
                Platform.runLater(() -> {
                    Label messageLabel = new Label("Received message from: " + message.getSender().getUserID());
                    mainMenuLayout.setCenter(new StackPane(messageLabel)); // Display message in UI
                });
            } catch (IOException e) {
                System.err.println("Error handling message: " + e.getMessage());
            }
        }

    }
}
