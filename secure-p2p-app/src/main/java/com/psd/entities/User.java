package com.psd.entities;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user in the decentralized P2P network.
 * Each user has their own private and public key pair for encryption, and can communicate
 * with other users in the network.
 */
public class User {
    
    // Unique identifier for the user
    private String userID;

    // Public and private key pair used for encryption and signing
    private PublicKey publicKey;
    private PrivateKey privateKey;

    // IP address of the user in the P2P network
    private String ipAddress;

    // List of contacts (other users) the user communicates with
    private List<User> contacts;

    // List of conversations this user is involved in
    private List<Conversation> conversations;

    /**
     * Constructs a new User with the specified ID, keys, and IP address.
     * 
     * @param userID     A unique identifier for the user.
     * @param publicKey  The public key used for encryption.
     * @param privateKey The private key used for decryption and signing.
     * @param ipAddress  The IP address of the user.
     */
    public User(String userID, PublicKey publicKey, PrivateKey privateKey, String ipAddress) {
        this.userID = userID;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.ipAddress = ipAddress;
        this.contacts = new ArrayList<>();
        this.conversations = new ArrayList<>();
    }

    public User(String userID, String ipAddress) {
        this.userID = userID;
        this.ipAddress = ipAddress;
        this.contacts = new ArrayList<>();
        this.conversations = new ArrayList<>();
    }

    /**
     * Sends a message to another user.
     * The message will be encrypted using the receiver's public key.
     * 
     * @param recipient The recipient user to send the message to.
     * @param message   The message to send.
     */
    public void sendMessage(User recipient, Message message) {
        //TODO
    }

    /**
     * Receives a message from another user.
     * The message will be decrypted using this user's private key.
     * 
     * @param message The encrypted message received.
     */
    public void receiveMessage(Message message) {
        //TODO
    }

    /**
     * Starts a new conversation with another user.
     * 
     * @param otherUser The user to start the conversation with.
     * @return A new conversation object.
     */
    public Conversation startConversation(User otherUser) {
        //TODO
        return null;
    }

    /**
     * Adds a new contact (user) to the contact list.
     * 
     * @param contact The new contact to be added.
     */
    public void addContact(User contact) {
        //TODO
    }

    // Getters and setters for user attributes
    
    /**
     * Returns the user's unique identifier.
     * 
     * @return The userID of the user.
     */
    public String getUserID() {
        return userID;
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
     * Returns the list of contacts the user has.
     * 
     * @return A list of contacts.
     */
    public List<User> getContacts() {
        return contacts;
    }

    /**
     * Returns the list of conversations this user is part of.
     * 
     * @return A list of conversations.
     */
    public List<Conversation> getConversations() {
        return conversations;
    }

}
