package com.psd.entities;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Represents a conversation between two users.
 * A conversation consists of a series of messages exchanged between the users.
 */
public class Conversation {

    // The two users involved in the conversation
    private User participant1;
    private User participant2;

    // A list of messages exchanged in the conversation
    private List<Message> messages;

    // Timestamp of when the conversation started
    private LocalDateTime startTime;

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
     * Returns the participants in the conversation.
     * 
     * @return A list containing the two participants in the conversation.
     */
    public List<User> getParticipants() {
        List<User> pList = new ArrayList<>();
        pList.add(participant1);
        pList.add(participant2);
        return pList;
    }

    /**
     * Returns the timestamp of when the conversation started.
     * 
     * @return The start time of the conversation.
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Returns the other participant in the conversation, given a user.
     * 
     * @param user The user for whom to find the other participant.
     * @return The other participant in the conversation, or null if the user is not part of the conversation.
     */
    public User getOtherParticipant(User user) {
        //TODO
        return null;
    }

    /**
     * Checks if the given user is a participant in the conversation.
     * 
     * @param user The user to check.
     * @return True if the user is a participant, false otherwise.
     */
    public boolean isParticipant(User user) {
        //TODO
        return false;
    }

    /**
     * Returns a formatted string representation of the conversation.
     * Shows the participants and the list of messages exchanged.
     * 
     * @return A string representation of the conversation.
     */
    @Override
    public String toString() {
        //TODO
        return null;
    }
}
