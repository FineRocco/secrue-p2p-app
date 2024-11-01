/**
 * The {@code P2PClient} class facilitates secure peer-to-peer (P2P) communication 
 * for a user in a decentralized messaging application. It enables the user to 
 * send their own data to a central server and transmit messages directly to other 
 * users over SSL/TLS. 
 *
 * <p>Main functionalities include:
 * <ul>
 *   <li>Sending user information to a central server for registration or updates</li>
 *   <li>Establishing secure connections to other peers for direct messaging</li>
 * </ul>
 *
 * <p>Dependencies: This class relies on {@code EncriptionService} for SSL/TLS configuration 
 * and {@code SerializationService} for serializing and deserializing user data.
 */
package com.psd;

import com.psd.entities.User;
import com.psd.services.EncriptionService;
import com.psd.services.SerializationService;

import javax.net.ssl.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.security.Security;

public class P2PClient {

    private User user;
    private SSLSocketFactory sslSocketFactory;
    private SSLSocket clientSocket;

    static {
        // Register the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Initializes the {@code P2PClient} by setting up an SSL context and creating 
     * an {@link SSLSocketFactory} for secure connections. It serializes the user 
     * data and sends it to the central server upon creation.
     *
     * @param user The {@link User} object representing the client.
     */
    public P2PClient(User user) {
        this.user = user;
        sslSocketFactory = EncriptionService.initializeSSLContext(user.getUserID()); // Initialize SSL context when client is created
        if (sslSocketFactory == null) {
            System.out.println("Failed to create SSL peer client socket factory.");
            return;
        }

        // Serialize the user object and send it to the central server
        List<byte[]> serializedUsers = new ArrayList<>();
        serializedUsers.add(SerializationService.serialize(user));
        sendUserToCentralServer(serializedUsers);
    }

    /**
     * Sends a list of serialized user data to the central server over SSL.
     *
     * @param users List of serialized user objects in byte array format.
     */
    public void sendUserToCentralServer(List<byte[]> users) {
        try {
            sslSocketFactory = EncriptionService.initializeSSLContext(user.getUserID());
            // Connect to central server over SSL
            clientSocket = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getLocalHost(), 8888);

            // Set up output stream for sending data
            DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());

            // Send the number of user objects
            dataOut.writeInt(users.size());

            // Send each user object
            for (byte[] user : users) {
                dataOut.writeInt(user.length); // Send length of user data
                dataOut.write(user); // Send user data
            }

            dataOut.flush();

        } catch (IOException e) {
            System.out.println("Error connecting to central server: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to central server", e);
        }
    }

    /**
     * Sends a message to a specified user by establishing an SSL connection 
     * to the receiver's IP and port.
     *
     * @param receiver The {@link User} representing the message receiver.
     * @param message The message to be sent as a byte array.
     * @throws IOException if an I/O error occurs during message sending.
     */
    public void sendMessage(User receiver, byte[] message) {
        try {
            sslSocketFactory = EncriptionService.initializeSSLContext(user.getUserID());

            // Connect to the receiver's IP and port over SSL
            clientSocket = (SSLSocket) sslSocketFactory.createSocket(receiver.getIpAddress(), receiver.getPort());

            // Initialize output stream for sending message
            DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());
            dataOut.writeInt(message.length); // Send message length
            dataOut.write(message); // Send message data
            dataOut.flush();
        } catch (IOException e) {
            System.out.println("Error connecting to receiver: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to receiver", e);
        }
    }
}
