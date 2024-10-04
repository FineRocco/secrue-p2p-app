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

    // The encrypted content of the message
    private String encryptedContent;

    // The actual (plain) content of the message before encryption (not stored after encryption)
    private String content;

    // The timestamp of when the message was created
    private LocalDateTime timestamp;

    // The digital signature of the message (for authenticity and integrity)
    private String signature;

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

    /**
     * Encrypts the content of the message using the recipient's public key.
     * 
     * @param content    The plain-text message content to encrypt.
     * @param publicKey  The public key of the recipient.
     * @return The encrypted content as a string.
     */
    public String encryptMessage(String content, PublicKey publicKey) {
        //TODO
        return null;
    }

    /**
     * Decrypts the content of the message using the recipient's private key.
     * 
     * @param encryptedContent The encrypted message content.
     * @param privateKey       The private key of the recipient.
     * @return The decrypted plain-text content of the message.
     */
    public String decryptMessage(String encryptedContent, PrivateKey privateKey) {
        //TODO
        return null;
    }

    /**
     * Signs the message using the sender's private key to ensure integrity and authenticity.
     * 
     * @param privateKey The private key of the sender.
     * @return The digital signature of the message.
     */
    public String signMessage(PrivateKey privateKey) {
        //TODO
        return null;
    }

    /**
     * Verifies the signature of the message using the sender's public key.
     * This ensures that the message has not been tampered with and was indeed sent by the claimed sender.
     * 
     * @param publicKey The public key of the sender.
     * @return True if the signature is valid, false otherwise.
     */
    public boolean verifySignature(PublicKey publicKey) {
        //TODO
        return false;
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
     * Returns the encrypted content of the message.
     * 
     * @return The encrypted message content.
     */
    public String getEncryptedContent() {
        return encryptedContent;
    }

    /**
     * Sets the encrypted content of the message.
     * 
     * @param encryptedContent The encrypted message content.
     */
    public void setEncryptedContent(String encryptedContent) {
        this.encryptedContent = encryptedContent;
    }

    /**
     * Returns the timestamp when the message was created.
     * 
     * @return The timestamp of message creation.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the digital signature of the message.
     * 
     * @return The digital signature as a base64 string.
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Sets the digital signature of the message.
     * 
     * @param signature The digital signature of the message.
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }
}
