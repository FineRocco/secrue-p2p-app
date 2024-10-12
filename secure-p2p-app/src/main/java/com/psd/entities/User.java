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
    
    // Unique identifier for the user
    private String userID;

    //Username
    private String username;

    // Public and private key pair used for encryption and signing
    private PublicKey publicKey;
    private PrivateKey privateKey;

    // IP address and Port of the user in the P2P network
    private String ipAddress;
    private int port;

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
    public User(String userName, PublicKey publicKey, PrivateKey privateKey, String ipAddress, int port) {
        this.userID = userName + publicKey.toString();
        this.username = userName;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.ipAddress = ipAddress;
        this.port = port;
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
        contacts.add(contact);
    }

    public User findContactByUsername(String username) {
        for (User contact : contacts) {
            if (contact.getUserName().equalsIgnoreCase(username)) {
                return contact;
            }
        }
        return null;
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

    /**
     * Returns the list of contacts the user has.
     * 
     * @return A list of contacts.
     */
    public List<User> getContacts() {
        return contacts;
    }

    /**
     * Shows the list of contacts with their username, IP address, and port.
     */
    public void showContacts() {
        if (contacts.isEmpty()) {
            System.out.println("No contacts found.");
        } else {
            System.out.println("Contacts:");
            for (User contact : contacts) {
                System.out.println("Username: " + contact.getUserName() + " | IP Address: " + contact.getIpAddress() + " | Port: " + contact.getPort());
            }
        }
    }

    /**
     * Returns the list of conversations this user is part of.
     * 
     * @return A list of conversations.
     */
    public List<Conversation> getConversations() {
        return conversations;
    }

    @Override
    public String toString() {
        return username + " (" + ipAddress + ":" + port + ")";
    }

}
