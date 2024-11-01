package com.psd.entities;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * The {@code Message} class represents a message exchanged between users in a 
 * peer-to-peer (P2P) network. Each message has unique identifiers for tracking, 
 * details about the sender and receiver, the message content, and a timestamp 
 * indicating when it was created.
 *
 * <p>Main functionalities include:
 * <ul>
 *   <li>Storing sender and receiver information</li>
 *   <li>Storing the message content and creation timestamp</li>
 *   <li>Retrieving unique identifiers for tracking the message</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 *     Message message = new Message("msg001", senderUser, receiverUser, "Hello, world!");
 *     String content = message.getContent();
 * </pre>
 */
public class Message implements Serializable {

    // Unique identifier for the message
    private final String messageID;

    // User who sent the message
    private final User sender;

    // User who will receive the message
    private final User receiver;

    // The actual (plain) content of the message
    private final String content;

    // The timestamp of when the message was created
    private final LocalDateTime timestamp;

    /**
     * Constructs a new {@code Message}.
     *
     * @param messageID A unique identifier for the message.
     * @param sender    The {@link User} sending the message.
     * @param receiver  The {@link User} receiving the message.
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
     * Returns the unique identifier of the message.
     *
     * @return The {@code messageID} of this message.
     */
    public String getMessageID() {
        return messageID;
    }

    /**
     * Returns the sender of the message.
     *
     * @return The {@link User} who sent the message.
     */
    public User getSender() {
        return sender;
    }

    /**
     * Returns the receiver of the message.
     *
     * @return The {@link User} who will receive the message.
     */
    public User getReceiver() {
        return receiver;
    }

    /**
     * Returns the plain content of the message.
     *
     * @return The plain-text content of this message.
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the timestamp when the message was created.
     *
     * @return The {@link LocalDateTime} timestamp of message creation.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
