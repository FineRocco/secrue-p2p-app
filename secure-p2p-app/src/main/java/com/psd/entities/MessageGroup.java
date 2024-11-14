package com.psd.entities;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * The {@code MessageGroup} class represents a message sent to a group in a peer-to-peer (P2P) network.
 * Each group message includes unique identifiers for tracking, details about the sender, the group,
 * the message content, and a timestamp indicating when it was created.
 *
 * <p>Main functionalities include:
 * <ul>
 *   <li>Storing sender and group information</li>
 *   <li>Storing the message content and creation timestamp</li>
 *   <li>Retrieving unique identifiers for tracking the message</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 *     MessageGroup messageGroup = new MessageGroup("msgGroup001", senderUser, group, "Hello, group!");
 *     String content = messageGroup.getContent();
 * </pre>
 */
public class MessageGroup implements Serializable {
    // Define a fixed serialVersionUID
    private static final long serialVersionUID = 1L;

    // Unique identifier for the group message
    private final String messageID;

    // User who sent the message
    private final User sender;

    // Group to which the message is sent
    private final String groupId;

    // The actual (plain) content of the message
    private final String content;

    // The timestamp of when the message was created
    private final Instant timestamp;

    /**
     * Constructs a new {@code MessageGroup}.
     *
     * @param messageID A unique identifier for the message.
     * @param sender    The {@link User} sending the message.
     * @param groupId     The receiving the message.
     * @param content   The plain-text content of the message.
     */
    public MessageGroup(User sender, String groupId, String content) {
        this.messageID = UUID.randomUUID().toString();
        this.sender = sender;
        this.groupId = groupId;
        this.content = content;
        this.timestamp = Instant.now();
    }

    /**
     * Returns the unique identifier of the message.
     *
     * @return The {@code messageID} of this group message.
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
     * Returns the group that will receive the message.
     *
     * @return The {@link Group} to which this message is sent.
     */
    public String getGroupId() {
        return groupId;
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
    public Instant getTimestamp() {
        return timestamp;
    }
}
