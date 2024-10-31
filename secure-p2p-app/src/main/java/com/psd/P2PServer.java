package com.psd;

import com.psd.entities.Conversation;
import com.psd.entities.Message;
import com.psd.entities.User;
import com.psd.services.EncriptionService;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import javax.net.ssl.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.psd.entities.Message;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
 * Each user has their own P2PServer instance listening on a specified port.
 */
public class P2PServer {

    private static final Map<String, Conversation> conversations = new HashMap<>();
    private static final Object userAuxLock = new Object();
    private static User userAux;
    private final User user;
    private final int port;// The port on which the server listens
    private SSLServerSocket serverSocket;
    private volatile boolean running = true; // Will be modified by different threads
    private BorderPane mainMenuLayout;  // Reference to the main layout in the JavaFX UI
    private P2PClient client;
    private SSLServerSocketFactory sslServerSocketFactory;

    static {
        // Register the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    public P2PServer(User user, BorderPane mainMenuLayout) {
        this.user = user;
        this.port = user.getPort();
        this.mainMenuLayout = mainMenuLayout;
        this.client = new P2PClient(user);
        initializeServerKeys(); // Generate keys and import certificates
        initializeSSLContext(); // Load SSL context with the latest truststore
        new Thread(this::start).start(); // Start server on a new thread


    }

    private static Object deserializeMessage(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(byteIn)) {
            return in.readObject();
        }
    }


    /**
     * Creates a unique key for each conversation between two users.
     */
    private static String getConversationKey(User user1, User user2) {
        return (user1.getUserID().compareTo(user2.getUserID()) < 0)
                ? user1.getUserID() + "-" + user2.getUserID()
                : user2.getUserID() + "-" + user1.getUserID();
    }

    /**
     * Starts the server to listen for incoming messages on the specified port.
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
    private void initializeServerKeys() {
        try {
            KeyPair keyPair = EncriptionService.generateKeyPair();
            X509Certificate cert = EncriptionService.generateSelfSignedCertificate(keyPair);

            // Save peer's keystore with its own key pair and certificate
            EncriptionService.saveKeyStore(keyPair, cert, user.getUserID());

            // Import peer certificate into server's truststore
            EncriptionService.importCertToTruststore(cert, "server-truststore.jks", user.getUserID());

            // Reload SSL context to include the newly imported certificate
            initializeSSLContext();

        } catch (Exception e) {
            System.out.println("Error generating keys or importing to truststore: " + e.getMessage());
        }
    }
    /**
     * Configures SSL context using keystore and truststore.
     */
    private void initializeSSLContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");

            try (InputStream keyStoreStream = new FileInputStream(EncriptionService.getStoreDirectory() + user.getUserID() + "-keystore.jks")) {
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

    /**
     * Sends a direct message from the sender to the receiver.
     */
    public boolean sendDirectMessage(Message message, User sender, User receiver) {
        try {
            // Check if receiver details need to be updated from the server
            if (receiver.getIpAddress() == null || receiver.getPort() == 0) {
                client.sendUserToCentralServer(List.of(serializeMessage(sender), serializeMessage(receiver)));
                synchronized (userAuxLock) {
                    userAuxLock.wait(); // Wait for receiver details update
                    receiver = userAux;
                }
            }
            User finalReceiver = receiver;

            String conversationKey = getConversationKey(sender, finalReceiver);
            Conversation conversation = conversations.computeIfAbsent(conversationKey, key -> new Conversation(sender, finalReceiver));

            // Add message to conversation and send it
            conversation.addMessage(message);
            client.sendMessage(finalReceiver, serializeMessage(message));
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
            return false;
        }
    }

    private byte[] serializeMessage(Object message) throws IOException {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(message);
            return byteOut.toByteArray();
        }
    }

    /**
     * Retrieves all conversations involving the specified user.
     */
    public List<Conversation> getAllConversations(User user) {
        List<Conversation> userConversations = new ArrayList<>();
        for (Conversation conversation : conversations.values()) {
            if (conversation.isParticipant(user)) {
                userConversations.add(conversation);
            }
        }
        return userConversations;
    }

    /**
     * Handles communication with an individual client in a separate thread.
     */
    private static class ClientHandler implements Runnable {
        private final SSLSocket socket;
        private final BorderPane mainMenuLayout;

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
                Object receivedObject = deserializeMessage(messageBytes);
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
         * Updates the userAux object with new user details.
         */
        private void updateUserAux(User user) {
            synchronized (userAuxLock) {
                userAux = user;
                userAuxLock.notifyAll(); // Notify waiting threads of the update
            }
        }

        /**
         * Processes a received message and updates the conversation and UI.
         */
        private void handleMessage(Message message) {
            System.out.printf("Received message from %s:%d%n", message.getSender().getIpAddress(), message.getSender().getPort());

            String conversationKey = getConversationKey(message.getSender(), message.getReceiver());
            Conversation conversation = conversations.computeIfAbsent(conversationKey, key -> new Conversation(message.getSender(), message.getReceiver()));
            conversation.addMessage(message);

            Platform.runLater(() -> {
                Label messageLabel = new Label("Received message from: " + message.getSender().getUserID());
                mainMenuLayout.setCenter(new StackPane(messageLabel)); // Display message in UI
            });
        }
    }
}
