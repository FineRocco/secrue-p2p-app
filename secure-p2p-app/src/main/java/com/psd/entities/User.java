package com.psd.entities;

import java.io.Serializable;

/**
 * Represents a user in the P2P network.
 */
public class User implements Serializable {
    private final String userID; // Unique identifier for the user

    // IP address and Port of the user in the P2P network
    private final String ipAddress;
    private final int port;

    /**
     * Constructs a new User with the specified ID, keys, and IP address.
     *
     * @param userName  A unique identifier for the user.
     * @param ipAddress The IP address of the user.
     * @param port      The port used by the user.
     */

    public User(String userName, String ipAddress, int port) {
        this.userID = userName;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Returns the user's unique identifier.
     *
     * @return The userID of the user.
     */
    public String getUserID() {
        return userID;
    }

    /**
     * Returns the user's IP address.
     *
     * @return The IP address of the user.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns the user's IP address.
     *
     * @return The IP address of the user.
     */
    public int getPort() {
        return port;
    }


}
