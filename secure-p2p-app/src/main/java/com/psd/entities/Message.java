package com.psd.entities;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * Represents a message being exchanged between users in the P2P network.
 * Each message contains encrypted content, a signature, and metadata like the sender and recipient.
 */
public class Message implements Serializable{

    // Unique identifier for the message
    private String messageID;

    // UserID of the sender
    private User sender;

    // UserID of the receiver
    private User receiver;

    // The actual (plain) content of the message before encryption (not stored after encryption)
    private String content;

    // The timestamp of when the message was created
    private LocalDateTime timestamp;


    /**
     * Constructs a new Message.
     * 
     * @param messageID A unique identifier for the message.
     * @param sender    The user sending the message.
     * @param receiver  The user receiving the message.
     * @param content   The plain-text content of the message.
     */
    public Message(String messageID, User sender, User receiver, String content) {
        this.messageID = messageID;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters for message attributes

    /**
     * Returns the unique identifier of the message.
     *
     * @return The messageID.
     */
    public String getMessageID() {
        return messageID;
    }

    /**
     * Returns the sender of the message.
     * 
     * @return The user who sent the message.
     */
    public User getSender() {
        return sender;
    }

    /**
     * Returns the receiver of the message.
     * 
     * @return The user who will receive the message.
     */
    public User getReceiver() {
        return receiver;
    }

    /**
     * Returns the plain content of the message before encryption.
     * 
     * @return The plain-text content of the message.
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the plain content of the message (for decryption purposes).
     * 
     * @param content The plain-text content of the message.
     */
    public void setContent(String content) {
        this.content = content;
    }


    /**
     * Returns the timestamp when the message was created.
     * 
     * @return The timestamp of message creation.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

}
