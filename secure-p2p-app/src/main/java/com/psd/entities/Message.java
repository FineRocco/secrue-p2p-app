package com.psd.entities;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * The {@code Message} class represents a message exchanged between users in a
 * peer-to-peer (P2P) network. Each message has unique identifiers for tracking,
 * details about the sender and receiver, the message content, and a timestamp
 * indicating when it was created.
 */
public class Message implements Serializable {
    // Define a fixed serialVersionUID
    private static final long serialVersionUID = 1L;

    // Unique identifier for the message
    private String messageID;

    // User who sent the message
    private User sender;

    // User who will receive the message
    private User receiver;

    // The actual (plain) content of the message
    private String content;

    // The timestamp of when the message was created
    private String timestamp;

    /**
     * No-argument constructor required for deserialization.
     * Initializes fields with default values.
     */
    public Message() {

    }

    /**
     * Factory method to create a new {@code Message} with a specific timestamp.
     *
     * @param sender    The {@link User} sending the message.
     * @param receiver  The {@link User} receiving the message.
     * @param content   The plain-text content of the message.
     * @return A new {@code Message} instance with the specified timestamp.
     */
    public static Message createMessage(User sender, User receiver, String content) {
        Message message = new Message();
        message.messageID = UUID.randomUUID().toString();
        message.sender = sender;
        message.receiver = receiver;
        message.content = content;
        String timestampString = Instant.now().toString();
        message.timestamp = timestampString;
        return message;
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
     * @return The {@link Instant} timestamp of message creation.
     */
    public String getTimestamp() {
        return timestamp;
    }
}
