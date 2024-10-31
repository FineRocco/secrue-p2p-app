package com.psd.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a conversation between two users.
 * A conversation consists of a series of messages exchanged between the users.
 */
public class Conversation {

    // The two users involved in the conversation
    private final User participant1;
    private final User participant2;

    // A list of messages exchanged in the conversation
    private final List<Message> messages;

    // Timestamp of when the conversation started
    private final LocalDateTime startTime;

    /**
     * Constructs a new Conversation between two users.
     *
     * @param participant1 The first user in the conversation.
     * @param participant2 The second user in the conversation.
     */
    public Conversation(User participant1, User participant2) {
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.messages = new ArrayList<>();
        this.startTime = LocalDateTime.now();
    }

    /**
     * Adds a message to the conversation.
     *
     * @param message The message to be added.
     */
    public void addMessage(Message message) {
        messages.add(message);
    }

    /**
     * Returns the list of all messages in the conversation.
     *
     * @return A list of messages exchanged in this conversation.
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Returns the participant1 in the conversation.
     *
     * @return An user from the conversation.
     */
    public User getParticipant1() {
        return participant1;
    }

    /**
     * Returns the participant2 in the conversation.
     *
     * @return An user from the conversation.
     */
    public User getParticipant2() {
        return participant2;
    }

    /**
     * Checks if the specified user is a participant in the conversation.
     *
     * @param user The user to check.
     * @return True if the user is a participant, false otherwise.
     */
    public boolean isParticipant(User user) {
        return participant1.getUserID().equals(user.getUserID()) || participant2.getUserID().equals(user.getUserID());
    }

    /**
     * Returns the timestamp of when the conversation started.
     *
     * @return The start time of the conversation.
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

}
