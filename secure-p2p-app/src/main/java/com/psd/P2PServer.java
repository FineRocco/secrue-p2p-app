package com.psd;

import com.psd.entities.Conversation;
import com.psd.entities.User;
import com.psd.services.EncriptionService;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;

import javax.net.ssl.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.psd.entities.Message;

import java.io.*;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a P2P server that listens for incoming messages from peers.
 * Each user has their own P2PServer running, which listens on the specified port.
 */
public class P2PServer {

    private static User user;
    private int port;  // The port on which the server listens
    private SSLServerSocket serverSocket;
    private volatile boolean running = true; // Will be modified by different threads
    private BorderPane mainMenuLayout;  // Reference to the main layout in the JavaFX UI
    private static Map<String, Conversation> conversations;
    private P2PClient client;
    private SSLServerSocketFactory sslServerSocketFactory;

    static {
        // Register the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    public P2PServer(User user, BorderPane mainMenuLayout) {
        P2PServer.user = user;
        this.port = user.getPort();
        this.mainMenuLayout = mainMenuLayout;
        this.conversations = new HashMap<>();
        System.out.println("ola");

        initializeServerKeys(); // Generate keys and import certificates
        initializeSSLContext(); // Load SSL context with the latest truststore

        new Thread(() -> {
            start();  // This will listen in the background
        }).start();
    }

    /**
     * Starts the server to listen for incoming messages on the specified port.
     *
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws UnrecoverableKeyException
     * @throws CertificateException
     */
    public void start() {
        try {
            if (sslServerSocketFactory == null) {
                System.out.println("Failed to create SSL peer server socket factory.");
                return;
            }

            serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port, 50, InetAddress.getByName(user.getIpAddress()));
            System.out.println("P2PServer: Created socket with this data: " + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort());

            this.client = new P2PClient(user); // Initialize client to send messages

            while (running) {
                SSLSocket socket = (SSLSocket) serverSocket.accept(); // Accept incoming connection
                new Thread(new ClientHandler(socket, mainMenuLayout)).start(); // Handle each client in a new thread
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeServerKeys() {
        try {
            KeyPair keyPair = EncriptionService.generateKeyPair();
            X509Certificate cert = EncriptionService.generateSelfSignedCertificate(keyPair);

            // Save peer's keystore with its own key pair and certificate
            EncriptionService.saveKeyStore(keyPair, cert, user.getUserName());

            // Import peer certificate into server's truststore
            EncriptionService.importCertToTruststore(cert, "server-truststore.jks", user.getUserName());

            // Reload SSL context to include the newly imported certificate
            initializeSSLContext();

        } catch (Exception e) {
            System.out.println("Error generating keys or importing to truststore: " + e.getMessage());
        }
    }

    private void initializeSSLContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");

            try (InputStream keyStoreStream = new FileInputStream(EncriptionService.getStoreDirectory() + user.getUserName() + "-keystore.jks")) {
                ks.load(keyStoreStream, "centralServer".toCharArray());
            }
            kmf.init(ks, "centralServer".toCharArray());

            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream trustStoreStream = new FileInputStream(EncriptionService.getStoreDirectory() + "server-truststore.jks")) {
                trustStore.load(trustStoreStream, "centralServer".toCharArray());
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            sslServerSocketFactory = sslContext.getServerSocketFactory();

        } catch (Exception e) {
            System.out.println("Error creating server SSL context: " + e.getMessage());
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

            System.out.println("P2PClient: Send message to " + receiver.getIpAddress() + ":" + receiver.getPort());

            // Send the actual serialized message
            client.sendMessage(receiver, serializedMessage);

            return true;
        } catch (IOException e) {
            return false;
        }
    }


    private byte[] serializeMessage(Message message) throws IOException {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(message);
            return byteOut.toByteArray();
        }
    }

    private static Message deserializeMessage(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(byteIn)) {
            return (Message) in.readObject();
        }
    }

    /**
     * Creates a unique key for each conversation between two users.
     *
     * @param user1 The first participant
     * @param user2 The second participant
     * @return A string key representing the unique conversation
     */
    private static String getConversationKey(User user1, User user2) {
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
        for (Conversation conversation : conversations.values()) {
            if (conversation.getParticipant1().getUserID().equals(user.getUserID()) ||
                    conversation.getParticipant2().getUserID().equals(user.getUserID())) {
                userConversations.add(conversation);
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

                System.out.println("Clienthandler: Received message from " + message.getSender().getIpAddress() + ":" + message.getSender().getPort() );

                // Check if conversation exists between sender and receiver
                String conversationKey = getConversationKey(message.getSender(), message.getReceiver());
                Conversation conversation = conversations.get(conversationKey);

                if (conversation == null) {
                    // Create a new conversation if one does not exist
                    conversation = new Conversation(message.getSender(), message.getReceiver());
                    conversations.put(conversationKey, conversation);
                    System.out.println("New conversation created between " + message.getSender().getUserName() + " and " + message.getReceiver().getUserName());
                }

                // Add the message to the conversation
                conversation.addMessage(message);

                // Update the JavaFX UI on the JavaFX Application Thread
                Platform.runLater(() -> {
                    // Create a new label with the received message
                    Label messageLabel = new Label("Received message from: " + message.getSender().getUserName());

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
