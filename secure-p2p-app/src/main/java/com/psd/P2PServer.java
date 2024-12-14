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
import com.psd.entities.Share;
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
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a P2P server that listens for incoming messages from peers.
 * Each user has their own {@code P2PServer} instance listening on a specified port.
 */
public class P2PServer {

    private static final Object userAuxLock = new Object();
    public static BigInteger secretKeyCloud; // The secret key
    private static User userAux;
    private static AWS3Storage aws3Storage = AWS3Storage.getInstance();
    private static FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private static AzureBlobStorage azureBlobStorage = AzureBlobStorage.getInstance();

    static {
        // Register the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    private final User user;
    private final int port; // The port on which the server listens
    private SSLServerSocket serverSocket;
    private volatile boolean running = true; // Will be modified by different threads
    private BorderPane mainMenuLayout; // Reference to the main layout in the JavaFX UI
    private P2PClient client;
    private SSLServerSocketFactory sslServerSocketFactory;

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
        this.client = new P2PClient(user); // Initialize client

        // Create the user-specific storage if it doesn't exist
        aws3Storage.createBucketForUser(user.getUserID());
        firebaseStorage.createUserCollection(user.getUserID());
        azureBlobStorage.createUserContainerClient(user.getUserID());

        ensureKeySharesExist(user.getUserID()); // Ensure key shares exist in the clouds or creates it

        new Thread(this::start).start(); // Start server on a new thread
    }

    public static void saveConversationClouds(String conversationId, String encryptedConversation, String userId) {
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

    public static String loadConversationClouds(String conversationId, String userId) {
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
     * Saves a word to the user's dictionary in the cloud with associated metadata.
     *
     * @param userId         The ID of the user whose dictionary the word will be saved to.
     * @param word           The word to save.
     * @param messageContent The content of the message containing the word.
     * @param conversationId The ID of the conversation where the word was used.
     * @param timestamp      The timestamp when the word was sent.
     */
    public static void saveWordToDictionary(String userId, String word, String messageContent, String conversationId, String timestamp) {
        try {
            // Create a dictionary entry
            Map<String, List<String>> wordMetadata = new HashMap<>();
            wordMetadata.computeIfAbsent("messageContent", k -> new ArrayList<>()).add(messageContent);
            wordMetadata.computeIfAbsent("conversationId", k -> new ArrayList<>()).add(conversationId);
            wordMetadata.computeIfAbsent("timestamp", k -> new ArrayList<>()).add(timestamp);

            saveDicToCloud(userId.toLowerCase(), word, wordMetadata);
        } catch (Exception e) {
            System.err.println("Error saving word to dictionary for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Saves a word and its metadata to the dictionaries folder across all 3 clouds (AWS S3, Firebase, Azure).
     *
     * @param userId       The ID of the user whose dictionaries are being updated.
     * @param word         The word to save.
     * @param wordMetadata The metadata associated with the word.
     */
    public static void saveDicToCloud(String userId, String word, Map<String, List<String>> wordMetadata) {
        try {
            // Encrypt the word and metadata
            String encryptedWord = EncryptionService.encryptObject(secretKeyCloud, word);
            String encryptedMetadata = EncryptionService.encryptObject(secretKeyCloud, wordMetadata);

            // Check if dictionaries folder exists in all clouds
            boolean awsDicExists = aws3Storage.checkDicExists(userId);
            boolean firebaseDicExists = firebaseStorage.checkDicExists(userId);
            boolean azureDicExists = azureBlobStorage.checkDicExists(userId);

            // Create dictionaries folder if it doesn't exist
            if (!awsDicExists) {
                aws3Storage.createDictionariesFolder(userId);
            }
            if (!firebaseDicExists) {
                firebaseStorage.createDictionariesFolder(userId);
            }
            if (!azureDicExists) {
                azureBlobStorage.createDictionariesFolder(userId);
            }

            // Check if the word already exists in AWS S3
            if (aws3Storage.checkWordExistsInDic(userId, encryptedWord)) {
                // If the word exists, update its metadata
                String existingMetadata = aws3Storage.loadWordMetadata(userId, encryptedWord);
                // Decrypt the metadata to its original form
                Map<String, List<String>> metadata = (Map<String, List<String>>) EncryptionService.decryptObject(secretKeyCloud, existingMetadata);
                mergeMetadata(metadata, wordMetadata);
                String updatedMetadata = EncryptionService.encryptObject(secretKeyCloud, metadata);
                aws3Storage.saveWordToDic(userId, encryptedWord, updatedMetadata);
            } else {
                // Save new word and metadata if it doesn't exist
                aws3Storage.saveWordToDic(userId, encryptedWord, encryptedMetadata);
            }

            // Repeat the process for Firebase
            if (firebaseStorage.checkWordExistsInDic(userId, encryptedWord)) {
                String existingMetadata = firebaseStorage.loadWordMetadata(userId, encryptedWord);
                // Decrypt the metadata to its original form
                Map<String, List<String>> metadata = (Map<String, List<String>>) EncryptionService.decryptObject(secretKeyCloud, existingMetadata);
                mergeMetadata(metadata, wordMetadata);
                String updatedMetadata = EncryptionService.encryptObject(secretKeyCloud, metadata);
                firebaseStorage.saveWordToDic(userId, encryptedWord, updatedMetadata);
            } else {
                firebaseStorage.saveWordToDic(userId, encryptedWord, encryptedMetadata);
            }

            // Repeat the process for Azure
            if (azureBlobStorage.checkWordExistsInDic(userId, encryptedWord)) {
                String existingMetadata = azureBlobStorage.loadWordMetadata(userId, encryptedWord);
                // Decrypt the metadata to its original form
                Map<String, List<String>> metadata = (Map<String, List<String>>) EncryptionService.decryptObject(secretKeyCloud, existingMetadata);
                mergeMetadata(metadata, wordMetadata);
                String updatedMetadata = EncryptionService.encryptObject(secretKeyCloud, metadata);
                azureBlobStorage.saveWordToDic(userId, encryptedWord, updatedMetadata);
            } else {
                azureBlobStorage.saveWordToDic(userId, encryptedWord, encryptedMetadata);
            }

            System.out.println("Successfully saved or updated word and metadata in dictionaries across all clouds.");
        } catch (Exception e) {
            System.err.println("Error saving or updating word and metadata in dictionaries: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Merges new metadata into existing metadata without overwriting.
     *
     * @param existingMetadata The existing metadata map.
     * @param newMetadata      The new metadata to add.
     */
    private static void mergeMetadata(Map<String, List<String>> existingMetadata, Map<String, List<String>> newMetadata) {
        for (Map.Entry<String, List<String>> entry : newMetadata.entrySet()) {
            existingMetadata.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }
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

            if (awsShareExists && firebaseShareExists && azureShareExists) {
                System.out.println("All shares exist. Reconstructing the key...");

                BigInteger share1 = aws3Storage.loadKeyShare(userId, "share1");
                BigInteger share2 = firebaseStorage.loadKeyShare(userId, "share2");
                BigInteger share3 = azureBlobStorage.loadKeyShare(userId, "share3");

                // Combine the retrieved shares into a map
                Share[] retrievedShares = new Share[]{
                        new Share(BigInteger.valueOf(1), share1),
                        new Share(BigInteger.valueOf(2), share2),
                        new Share(BigInteger.valueOf(3), share3)
                };

                // Reconstruct the key
                this.secretKeyCloud = SecretSharingService.combine(retrievedShares);
                System.out.println("Reconstructed Secret Key: " + this.secretKeyCloud);
            } else {
                // Generate and split a new key if shares are missing
                System.out.println("Generating new key and shares...");
                this.secretKeyCloud = SecretSharingService.generateKey();
                System.out.println("Generated Secret Key: " + this.secretKeyCloud);

                Share[] shares = SecretSharingService.shares(secretKeyCloud);

                // Distribute shares to the clouds

                aws3Storage.saveKeyShare(userId, "share1", shares[0].getShare());
                System.out.println("Saved share1 to AWS.");

                firebaseStorage.saveKeyShare(userId, "share2", shares[1].getShare());
                System.out.println("Saved share2 to Firebase.");

                azureBlobStorage.saveKeyShare(userId, "share3", shares[2].getShare());
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

            // Ensure receiver details are up-to-date
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

            User finalReceiver = receiver;

            // Create the unique conversation ID based on sender and receiver
            String conversationId;
            try {
                conversationId = new Conversation(sender, finalReceiver).getConversationId();
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
                    conversation = new Conversation(sender, finalReceiver);
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

            // Split the message content into words and save them in the user's dictionary
            try {
                String[] words = message.getContent().split("\\s+"); // Split by whitespace
                for (String word : words) {
                    new Thread(() -> {
                        saveWordToDictionary(sender.getUserID(), word, message.getContent(), conversationId, message.getTimestamp());
                    }).start();
                }
                System.out.println("Words from message content saved to dictionaries.");
            } catch (Exception e) {
                System.err.println("Error saving words to dictionaries: " + e.getMessage());
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

    /**
     * Searches for messages containing a specific word across clouds for the current user.
     * Tries AWS S3 first, then Firebase, and finally Azure Blob Storage if the previous fails.
     *
     * @param currentUser The user whose dictionaries will be searched.
     * @param searchWord  The word to search for.
     * @return A map where the key is the metadata type (e.g., "messageId", "conversationId", "timestamp"),
     *         and the value is a list of associated values.
     */
    public Map<String, List<String>> searchMessagesByWord(User currentUser, String searchWord) {
        Map<String, List<String>> searchResults = new HashMap<>();
        String encryptedSearchWord;

        try {
            // Encrypt the search word to match cloud storage
            encryptedSearchWord = EncryptionService.encryptObject(secretKeyCloud, searchWord);
        } catch (Exception e) {
            System.err.println("Error encrypting search word: " + e.getMessage());
            e.printStackTrace();
            return searchResults; // Return empty results if encryption fails
        }

        try {
            // Attempt to retrieve from AWS S3
            if (aws3Storage.checkWordExistsInDic(currentUser.getUserID(), encryptedSearchWord)) {
                String encryptedMetadata = aws3Storage.loadWordMetadata(currentUser.getUserID(), encryptedSearchWord);
                mergeSearchResults(searchResults, encryptedMetadata);
                System.out.println("Successfully retrieved results from AWS S3.");
                return searchResults;
            }
        } catch (Exception e) {
            System.err.println("Error retrieving results from AWS S3: " + e.getMessage());
            System.out.println("Attempting to retrieve results from Firebase instead...");
        }

        try {
            // Attempt to retrieve from Firebase if AWS S3 fails
            if (firebaseStorage.checkWordExistsInDic(currentUser.getUserID(), encryptedSearchWord)) {
                String encryptedMetadata = firebaseStorage.loadWordMetadata(currentUser.getUserID(), encryptedSearchWord);
                mergeSearchResults(searchResults, encryptedMetadata);
                System.out.println("Successfully retrieved results from Firebase.");
                return searchResults;
            }
        } catch (Exception e) {
            System.err.println("Error retrieving results from Firebase: " + e.getMessage());
            System.out.println("Attempting to retrieve results from Azure Blob Storage instead...");
        }

        try {
            // Attempt to retrieve from Azure Blob Storage if both AWS S3 and Firebase fail
            if (azureBlobStorage.checkWordExistsInDic(currentUser.getUserID(), encryptedSearchWord)) {
                String encryptedMetadata = azureBlobStorage.loadWordMetadata(currentUser.getUserID(), encryptedSearchWord);
                mergeSearchResults(searchResults, encryptedMetadata);
                System.out.println("Successfully retrieved results from Azure Blob Storage.");
                return searchResults;
            }
        } catch (Exception e) {
            System.err.println("Error retrieving results from Azure Blob Storage: " + e.getMessage());
        }

        System.out.println("No results found for word: " + searchWord);
        return searchResults;
    }

    /**
     * Merges search results into the main result map.
     *
     * @param result            The main result map to update.
     * @param encryptedMetadata The encrypted metadata string to decrypt and parse.
     */
    private void mergeSearchResults(Map<String, List<String>> result, String encryptedMetadata) {
        try {
            // Decrypt the metadata
            Map<String, List<String>> metadata = (Map<String, List<String>>) EncryptionService.decryptObject(secretKeyCloud, encryptedMetadata);

            // Iterate over the metadata and add values to the result map
            for (Map.Entry<String, List<String>> entry : metadata.entrySet()) {
                String key = entry.getKey(); // Metadata key: messageContent, conversationId, timestamp
                List<String> values = entry.getValue(); // List of associated values

                // Add or merge values into the result map
                result.computeIfAbsent(key, k -> new ArrayList<>()).addAll(values);
            }
        } catch (Exception e) {
            System.err.println("Error merging search results: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendInterestsToCentralServer(User user) {
        client.sendInterestsToCentralServer(SerializationService.serialize(user));
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
            new Thread(() -> {
                User sender = message.getSender();
                User receiver = message.getReceiver();
                System.out.printf("Received message from %s:%d%n", sender.getIpAddress(), sender.getPort());

                // Create the unique conversation ID based on sender and receiver
                Conversation conversation = new Conversation(sender, receiver);
                String conversationId = conversation.getConversationId();

                // Attempt to load the conversation from AWS S3
                String encryptedConversation = loadConversationClouds(conversationId, receiver.getUserID());

                // If the conversation is still not found, create a new one
                if (encryptedConversation == null) {
                    System.out.println("Created a new conversation with ID: " + conversationId);

                    // Add message to conversation
                    conversation.addMessage(message);

                    try {
                        // Encrypt the conversation using the shared secret key
                        String newEncryptedConversation = EncryptionService.encryptObject(secretKeyCloud, conversation);

                        // Save updated conversation to both AWS S3, Firebase and AzureBlob
                        saveConversationClouds(conversationId, newEncryptedConversation, receiver.getUserID());
                    } catch (Exception e) {
                        // Handle exceptions (e.g., logging, retries, etc.)
                        System.err.println("Error processing conversation from cloud: " + e.getMessage());
                    }

                } else {

                    try {
                        // Decrypt the conversation using the shared secret key
                        Conversation conversationInCloud = (Conversation) EncryptionService.decryptObject(secretKeyCloud, encryptedConversation);

                        // Add message to conversation
                        conversationInCloud.addMessage(message);

                        // Encrypt the conversation using the shared secret key
                        String newEncryptedConversation = EncryptionService.encryptObject(secretKeyCloud, conversationInCloud);

                        // Save updated conversation to AWS S3, Firebase, and AzureBlob
                        saveConversationClouds(conversationId, newEncryptedConversation, receiver.getUserID());
                    } catch (Exception e) {
                        // Handle exceptions (e.g., logging, retries, etc.)
                        System.err.println("Error processing conversation from cloud: " + e.getMessage());
                    }
                }

                // Split the message content into words and save them in the user's dictionary
                try {
                    String[] words = message.getContent().split("\\s+"); // Split by whitespace
                    for (String word : words) {
                        new Thread(() -> {
                            saveWordToDictionary(receiver.getUserID(), word, message.getContent(), conversationId, message.getTimestamp());
                        }).start();
                    }
                    System.out.println("Words from message content saved to dictionaries.");
                } catch (Exception e) {
                    System.err.println("Error saving words to dictionaries: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();

            // Update the UI to display the received message
            Platform.runLater(() -> {
                Label messageLabel = new Label("Received message from: " + message.getSender().getUserID());
                mainMenuLayout.setCenter(new StackPane(messageLabel)); // Display message in UI
            });

        }
    }
}
