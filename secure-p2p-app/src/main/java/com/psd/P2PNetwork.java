package com.psd;

import com.psd.entities.*;
import javafx.scene.layout.BorderPane;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

/**
 * Represents the P2P network responsible for managing connections between peers (users) 
 * and facilitating communication in a decentralized manner.
 */
public class P2PNetwork {

    // Port
    private int port;

    // Track conversations based on participants' unique combination
    private Map<String, Conversation> conversations;

    public P2PNetwork(int port) {
        this.port = port;
        this.conversations = new HashMap<>();
    }

    /**
     * Connects a peer (user) to the P2P network by starting a P2PServer thread.
     * 
     * @param user The user that wants to join the network.
     * @param mainMenuLayout The layout where the received messages will be displayed.
     * @return True if the user is successfully connected, false otherwise.
     */
    public void connectPeer(User user, BorderPane mainMenuLayout) {
        // Start the peer's server in a new thread using the network's port
        new Thread(() -> {
            // Pass the mainMenuLayout to the P2PServer so that it can update the UI
            P2PServer server = new P2PServer(port, mainMenuLayout);
            server.start();  // This will listen in the background
        }).start();
    }

    /**
     * Sends a direct message from the sender to the receiver.
     * If no existing conversation exists, a new one is created.
     * 
     * @param message The message to send.
     * @param sender The user sending the message.
     * @param receiver The recipient of the message.
     * @param receiverPort The port the receiver is listening on.
     * @return True if the message was successfully sent, false otherwise.
     * @throws KeyStoreException 
     * @throws CertificateException 
     * @throws NoSuchAlgorithmException 
     * @throws KeyManagementException 
     */
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
     * Retrieves the conversation between two users if it exists.
     * 
     * @param user1 The first participant.
     * @param user2 The second participant.
     * @return The conversation if found, or null otherwise.
     */
    public Conversation getConversation(User user1, User user2) {
        return conversations.get(getConversationKey(user1, user2));
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
     * Disconnects a peer (user) from the P2P network.
     * 
     * @param user The user that wants to leave the network.
     * @return True if the user successfully disconnects, false otherwise.
     */
    public boolean disconnectPeer(User user) {
        // TODO
        return false;
    }

    /**
     * Finds a peer (user) on the network by their unique identifier.
     * 
     * @param userID The unique identifier of the peer.
     * @return The user found on the network, or null if the peer is not found.
     */
    public User findPeerByID(String userID) {
        // TODO
        return null;
    }

    /**
     * Broadcasts a message to all connected peers on the network.
     * 
     * @param message The message to broadcast.
     * @param sender The user sending the broadcast message.
     * @return True if the message was successfully broadcast, false otherwise.
     */
    public boolean broadcastMessage(Message message, User sender) {
        // TODO
        return false;
    }

    /**
     * Checks if a given user is currently connected to the network.
     * 
     * @param user The user to check.
     * @return True if the user is connected, false otherwise.
     */
    public boolean isPeerConnected(User user) {
        // TODO
        return false;
    }

    /**
     * Attempts to reconnect a peer to the network if they were disconnected due to failure or other issues.
     * 
     * @param user The user that needs to reconnect.
     * @return True if the reconnection was successful, false otherwise.
     */
    public boolean reconnectPeer(User user) {
        // TODO
        return false;
    }

    /**
     * Handles network failures or connection drops for peers.
     * Ensures messages are retried or rerouted if necessary.
     * 
     * @param user The user experiencing the failure.
     * @return True if the failure is handled successfully, false otherwise.
     */
    public boolean handleNetworkFailure(User user) {
        // TODO
        return false;
    }
}
