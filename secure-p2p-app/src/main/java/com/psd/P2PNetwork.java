package com.psd;

import com.psd.entities.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Represents the P2P network responsible for managing connections between peers (users) 
 * and facilitating communication in a decentralized manner.
 */
public class P2PNetwork {

    // List of active peers (users) connected to the network
    private List<User> activePeers;

    // A message queue for temporarily storing messages until they can be delivered
    private Queue<Message> messageQueue;

    //Port
    private int port;

    public P2PNetwork(int port) {
        this.port = port;
        this.activePeers = new ArrayList<>(); 
    }

    /**
     * Connects a peer (user) to the P2P network by starting a P2PServer thread.
     * 
     * @param user The user that wants to join the network.
     * @return True if the user is successfully connected, false otherwise.
     */
    public boolean connectPeer(User user) {
        if (!activePeers.contains(user)) {
            activePeers.add(user);
            // Start the peer's server in a new thread using the network's port
            new Thread(() -> {
                try {
                    P2PServer server = new P2PServer(port, user);
                    server.start();  // This will listen in the background
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            return true;
        }
        return false;
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
     * Sends a direct message from the sender to the receiver.
     * Since the User object no longer has a port, the receiver's port should
     * be specified by the caller or pre-configured.
     * 
     * @param message The message to send.
     * @param sender The user sending the message.
     * @param receiver The recipient of the message.
     * @param receiverPort The port the receiver is listening on.
     * @return True if the message was successfully sent, false otherwise.
     */
    public boolean sendDirectMessage(Message message, User sender, User receiver, int receiverPort) {
        try {
            // Create a client to send the message to the receiver's IP and port
            P2PClient client = new P2PClient(receiver.getIpAddress(), receiverPort);
            client.sendMessage(message.getContent());
            //client.closeConnection();  // Close after sending
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retrieves the list of all active peers connected to the P2P network.
     * 
     * @return A list of active peers on the network.
     */
    public List<User> getActivePeers() {
        // TODO
        return null;
    }

    /**
     * Synchronizes the network state among all active peers to ensure consistency.
     * This could be used to ensure messages and connections are up to date.
     * 
     * @return True if the synchronization is successful, false otherwise.
     */
    public boolean synchronizeNetwork() {
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
