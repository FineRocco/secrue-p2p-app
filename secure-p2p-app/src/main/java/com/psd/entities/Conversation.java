package com.psd.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code Conversation} class represents a conversation between two users 
 * in a peer-to-peer messaging application. It maintains the list of messages 
 * exchanged between the two users, as well as details about the participants and 
 * the start time of the conversation.
 *
 * <p>Main functionalities include:
 * <ul>
 *   <li>Adding messages to the conversation</li>
 *   <li>Retrieving the list of messages</li>
 *   <li>Checking if a user is a participant in the conversation</li>
 * </ul>
 */
public class Conversation implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String conversationId;
    private final User participant1;
    private final User participant2;
    private final List<Message> messages;
    private final LocalDateTime startTime;

    /**
     * Private constructor for a {@code Conversation} between two users with a generated ID.
     * This initializes the start time and an empty list of messages.
     *
     * @param conversationId The unique ID of the conversation.
     * @param participant1 The first user in the conversation.
     * @param participant2 The second user in the conversation.
     */
    private Conversation(String conversationId, User participant1, User participant2) {
        this.conversationId = conversationId;
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.messages = new ArrayList<>();
        this.startTime = LocalDateTime.now();
    }

    /**
     * Static factory method to create a new {@code Conversation} between two users.
     * Generates a unique conversation ID based on participant user IDs.
     *
     * @param participant1 The first user in the conversation.
     * @param participant2 The second user in the conversation.
     * @return A new {@code Conversation} object with a unique ID.
     */
    public static Conversation createConversation(User participant1, User participant2) {
        String conversationId = (participant1.getUserID().compareTo(participant2.getUserID()) < 0)
                ? participant1.getUserID() + "-" + participant2.getUserID()
                : participant2.getUserID() + "-" + participant1.getUserID();
        return new Conversation(conversationId, participant1, participant2);
    }

    /**
     * Adds a message to the conversation.
     *
     * @param message The {@link Message} to be added to the conversation.
     */
    public void addMessage(Message message) {
        messages.add(message);
    }

    /**
     * Returns the unique ID of the conversation.
     *
     * @return The unique conversation ID.
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * Returns the list of all messages in the conversation.
     *
     * @return A list of {@link Message} objects exchanged in this conversation.
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Returns the first participant in the conversation.
     *
     * @return The {@link User} representing the first participant.
     */
    public User getParticipant1() {
        return participant1;
    }

    /**
     * Returns the second participant in the conversation.
     *
     * @return The {@link User} representing the second participant.
     */
    public User getParticipant2() {
        return participant2;
    }

    /**
     * Checks if the specified user is a participant in the conversation.
     *
     * @param user The {@link User} to check.
     * @return {@code true} if the user is a participant, {@code false} otherwise.
     */
    public boolean isParticipant(User user) {
        return participant1.getUserID().equals(user.getUserID()) || participant2.getUserID().equals(user.getUserID());
    }

    /**
     * Returns the timestamp of when the conversation started.
     *
     * @return The {@link LocalDateTime} representing the start time of the conversation.
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }
}
