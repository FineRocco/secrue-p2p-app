package com.psd.entities;

import java.io.Serializable;
import java.util.Objects;

/**
 * The {@code User} class represents a user in a peer-to-peer (P2P) network.
 * Each user is uniquely identified by a user ID and includes network details
 * such as an IP address and port for establishing connections.
 *
 * <p>Main functionalities include:
 * <ul>
 *   <li>Storing user identification and network information</li>
 *   <li>Providing access to user details for establishing P2P connections</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 *     User user = new User("user123", "192.168.1.10", 8080);
 *     String userId = user.getUserID();
 * </pre>
 */
public class User implements Serializable {
    // Define a fixed serialVersionUID
    private static final long serialVersionUID = 1L;

    private final String userID; // Unique identifier for the user

    // IP address and port of the user in the P2P network
    private final String ipAddress;
    private final int port;

    /**
     * Constructs a new {@code User} with the specified ID, IP address, and port.
     *
     * @param userName  A unique identifier for the user.
     * @param ipAddress The IP address of the user in the network.
     * @param port      The port used by the user for connections.
     */
    public User(String userName, String ipAddress, int port) {
        this.userID = userName;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Returns the user's unique identifier.
     *
     * @return The {@code userID} of this user.
     */
    public String getUserID() {
        return userID;
    }

    /**
     * Returns the user's IP address.
     *
     * @return The IP address of this user.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns the user's port for network communication.
     *
     * @return The port used by this user.
     */
    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Check if both references are the same
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return Objects.equals(userID, user.userID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID);
    }
}
