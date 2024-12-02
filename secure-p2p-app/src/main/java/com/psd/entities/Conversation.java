package com.psd.entities;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The {@code Conversation} class represents a conversation between two users 
 * in a peer-to-peer messaging application. It maintains a list of messages 
 * exchanged between the two users, as well as details about the participants and 
 * the start time of the conversation.
 */
public class Conversation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String conversationId;
    private User participant1;
    private User participant2;
    private List<Message> messages; // Changed to List<Message>
    private String startTime; // Start time as ISO-8601 string

    /**
     * No-argument constructor required for deserialization.
     * Initializes fields with default values.
     */
    
    public Conversation(User participant1, User participant2) {
        String conversationId = (participant1.getUserID().compareTo(participant2.getUserID()) < 0)
                ? participant1.getUserID() + "-" + participant2.getUserID()
                : participant2.getUserID() + "-" + participant1.getUserID();

        this.conversationId = conversationId;
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.messages = new ArrayList<>();
        this.startTime = Instant.now().toString(); // Set the current time as ISO-8601 string
    }    

    /**
     * Handles deserialization of the `messages` field.
     * Ensures that `messages` is always stored as a `List<Message>`.
     *
     * @param messagesData The deserialized messages value, which could be a Map or a List.
     */
    @SuppressWarnings("unchecked")
    public void setMessages(Object messagesData) {
        if (messagesData instanceof List) {
            this.messages = (List<Message>) messagesData;
        } else if (messagesData instanceof Map) {
            this.messages = new ArrayList<>(((Map<String, Message>) messagesData).values());
        } else {
            System.err.println("Unsupported type for messages: " + messagesData.getClass());
            this.messages = new ArrayList<>(); // Default to empty list in case of errors
        }
    }

    /**
     * Handles deserialization of the `startTime` field.
     * Ensures that `startTime` is always stored as a `String`.
     *
     * @param startTime The deserialized startTime value, which could be a String or other type.
     */
    public void setStartTime(Object startTime) {
        if (startTime instanceof String) {
            this.startTime = (String) startTime;
        } else if (startTime instanceof Instant) {
            this.startTime = ((Instant) startTime).toString();
        } else {
            System.err.println("Unsupported type for startTime: " + startTime.getClass());
            this.startTime = Instant.now().toString(); // Default to current time in case of errors
        }
    }

    /**
     * Adds a message to the conversation.
     *
     * @param message The {@link Message} to be added to the conversation.
     */
    public void addMessage(Message message) {
        if (message != null) {
            messages.add(message);
        }
    }

    /**
     * Returns all messages in the conversation as a list.
     *
     * @return A list of {@link Message} objects in this conversation.
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Returns the unique ID of the conversation.
     *
     * @return The unique conversation ID.
     */
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * Returns the first participant in the conversation.
     *
     * @return The {@link User} representing the first participant.
     */
    public User getParticipant1() {
        return participant1;
    }

    public void setParticipant1(User participant1) {
        this.participant1 = participant1;
    }

    /**
     * Returns the second participant in the conversation.
     *
     * @return The {@link User} representing the second participant.
     */
    public User getParticipant2() {
        return participant2;
    }

    public void setParticipant2(User participant2) {
        this.participant2 = participant2;
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
     * Returns the timestamp of when the conversation started as a String.
     *
     * @return The start time as a String.
     */
    public String getStartTime() {
        return startTime;
    }
}
