package com.psd;

import com.psd.entities.Conversation;
import com.psd.entities.User;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;

import javax.net.ssl.*;

import com.psd.entities.Message;

import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a P2P server that listens for incoming messages from peers.
 * Each user has their own P2PServer running, which listens on the specified port.
 */
public class P2PServer {

    private int port;  // The port on which the server listens
    private SSLServerSocket serverSocket;
    private volatile boolean running = true;
    private BorderPane mainMenuLayout;  // Reference to the main layout in the JavaFX UI
    private static Map<String, Conversation> conversations;

    public P2PServer(int port, BorderPane mainMenuLayout) {
        this.port = port;
        this.mainMenuLayout = mainMenuLayout;  // Initialize the layout reference
        this.conversations = new HashMap<>();
        new Thread(() -> {
            start();  // This will listen in the background
        }).start();
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

    private static Message deserializeMessage(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
            ObjectInputStream in = new ObjectInputStream(byteIn)) {
            return (Message) in.readObject();
        }
    }
    public boolean sendDirectMessage(Message message, User sender, User receiver) {
        try {
            // Check if conversation exists between sender and receiver
            String conversationKey = getConversationKey(sender, receiver);
            Conversation conversation = conversations.get(conversationKey);

            if (conversation == null) {
                // Create a new conversation if one does not exist
                conversation = new Conversation(sender, receiver);
                conversations.put(conversationKey, conversation);
                System.out.println("New conversation created between " + sender.getUserName() + " and " + receiver.getUserName());
            }

            // Add the message to the conversation
            conversation.addMessage(message);

            // Serialize the message
            byte[] serializedMessage = serializeMessage(message);

            // Send the message using the P2P client
            P2PClient client = new P2PClient(receiver.getIpAddress(), receiver.getPort());

            // Send the actual serialized message
            client.sendMessage(serializedMessage);

            return true;

        } catch (IOException | KeyManagementException | NoSuchAlgorithmException | CertificateException | KeyStoreException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }
        return false;
    }

    private byte[] serializeMessage(Message message) throws IOException {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(message);
            return byteOut.toByteArray();
        }
    }

    /**
     * Creates a unique key for each conversation between two users.
     *
     * @param user1 The first participant
     * @param user2 The second participant
     * @return A string key representing the unique conversation
     */
    private String getConversationKey(User user1, User user2) {
        // Ensure consistent ordering to avoid duplicate keys
        if (user1.getUserID().compareTo(user2.getUserID()) < 0) {
            return user1.getUserID() + "-" + user2.getUserID();
        } else {
            return user2.getUserID() + "-" + user1.getUserID();
        }
    }

    /**
     * Retrieves all conversations involving the specified user.
     *
     * @param user The user whose conversations are to be retrieved.
     * @return A list of conversations involving the user.
     */
    public List<Conversation> getAllConversations(User user) {
        List<Conversation> userConversations = new ArrayList<>();
        for (Conversation convo : conversations.values()) {
            if (convo.getParticipant1().equals(user) || convo.getParticipant2().equals(user)) {
                userConversations.add(convo);
            }
        }
        return userConversations;
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
                Message message = deserializeMessage(messageBytes);
        
                // Update the JavaFX UI on the JavaFX Application Thread
                Platform.runLater(() -> {
                    // Create a new label with the received message
                    Label messageLabel = new Label("Received message: " + message.getContent());
        
                    // Set this label to the center of the mainMenuLayout
                    StackPane messagePane = new StackPane(messageLabel);
                    mainMenuLayout.setCenter(messagePane);  // Update the center with the received message
                });
        
                socket.close();  // Close the connection after the message is received
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }
}
