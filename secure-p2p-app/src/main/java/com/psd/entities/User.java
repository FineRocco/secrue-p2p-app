package com.psd.entities;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.Serializable;

/**
 * Represents a user in the decentralized P2P network.
 * Each user has their own private and public key pair for encryption, and can communicate
 * with other users in the network.
 */
public class User implements Serializable{

    private String userID; // Unique identifier for the user
    private String username;

    // Public and private key pair used for encryption and signing
    private PublicKey publicKey;
    private PrivateKey privateKey;

    // IP address and Port of the user in the P2P network
    private String ipAddress;
    private int port;

    /**
     * Constructs a new User with the specified ID, keys, and IP address.
     * 
     * @param userID     A unique identifier for the user.
     * @param publicKey  The public key used for encryption.
     * @param privateKey The private key used for decryption and signing.
     * @param ipAddress  The IP address of the user.
     */
    public User(String userName, PublicKey publicKey, PrivateKey privateKey, String ipAddress, int port) {
        this.userID = userName + publicKey.toString();
        this.username = userName;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public User(String userName, String ipAddress, int port) {
        this.username = userName;
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
     * Returns the user's unique identifier.
     * 
     * @return The userID of the user.
     */
    public String getUserName() {
        return username;
    }

    /**
     * Returns the public key of the user.
     * 
     * @return The public key.
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Returns the private key of the user.
     * 
     * @return The private key.
     */
    public PrivateKey getPrivateKey() {
        return privateKey;
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

    @Override
    public String toString() {
        return username + " (" + ipAddress + ":" + port + ")";
    }

}
