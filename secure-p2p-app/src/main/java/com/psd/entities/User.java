package com.psd.entities;

import java.io.Serializable;
import java.util.*;

/**
 * The {@code User} class represents a user in a peer-to-peer (P2P) network.
 * Each user is uniquely identified by a user ID and includes network details
 * such as an IP address, port for establishing connections, a list of interests,
 * and a map of group keys for groups the user has joined.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String userID; // Unique identifier for the user
    private final String ipAddress; // IP address in the P2P network
    private final int port; // Port used for connections
    private List<String> interests; // List of user's interests

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
        this.interests = new ArrayList<>(); // Initialize with an empty list
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

    public List<String> getInterests() {
        return new ArrayList<>(interests); // Return a copy to preserve immutability
    }

    public void setInterests(List<String> interests) {
        this.interests = new ArrayList<>(interests); // Set with a copy to preserve immutability
    }

    public void addInterest(String interest) {
        this.interests.add(interest);
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
                ", interests=" + interests +
                '}';
    }
}
