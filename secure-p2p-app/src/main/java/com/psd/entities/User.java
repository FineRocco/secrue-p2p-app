package com.psd.entities;

import java.io.Serializable;
import java.util.Objects;

/**
 * The {@code User} class represents a user in a peer-to-peer (P2P) network.
 * Each user is uniquely identified by a user ID and includes network details
 * such as an IP address, port for establishing connections, a list of interests,
 * and a map of group keys for groups the user has joined.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userID;
    private String ipAddress;
    private int port;


    /**
     * No-argument constructor required for deserialization.
     * Initializes fields with default values.
     */
    public User() {
        this.userID = null;
        this.ipAddress = null;
        this.port = 0; // Default port value
    }

    /**
     * Constructs a new {@code User} with the specified ID, IP address, and port.
     * Initializes an empty list of interests and group keys.
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

    // Getters and setters for group keys and interests

    public String getUserID() {
        return userID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return Objects.equals(userID, user.userID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID);
    }

    @Override
    public String toString() {
        return "User{" +
                "userID='" + userID + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                '}';
    }
}
