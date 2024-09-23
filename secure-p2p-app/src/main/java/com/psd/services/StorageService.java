package com.psd.services;

import com.psd.entities.*;
import java.util.List;

/**
 * Represents a service responsible for storing and retrieving messages, conversations, 
 * and other persistent data in the P2P messaging app.
 */
public class StorageService {

    // A list to store conversations in memory (can be replaced by a database or file storage)
    private List<Conversation> storedConversations;

    /**
     * Stores a conversation in the storage system.
     * 
     * @param conversation The conversation to store.
     * @return True if the conversation is successfully stored, false otherwise.
     */
    public boolean storeConversation(Conversation conversation) {
        // TODO
        return false;
    }

    /**
     * Retrieves a stored conversation between two users.
     * 
     * @param user1 The first participant of the conversation.
     * @param user2 The second participant of the conversation.
     * @return The retrieved conversation, or null if not found.
     */
    public Conversation retrieveConversation(User user1, User user2) {
        // TODO
        return null;
    }

    /**
     * Stores a message in the storage system, associating it with the correct conversation.
     * 
     * @param message The message to store.
     * @param conversation The conversation the message belongs to.
     * @return True if the message is successfully stored, false otherwise.
     */
    public boolean storeMessage(Message message, Conversation conversation) {
        // TODO
        return false;
    }

    /**
     * Retrieves all messages from a specific conversation.
     * 
     * @param conversation The conversation to retrieve messages from.
     * @return A list of messages from the specified conversation.
     */
    public List<Message> retrieveMessages(Conversation conversation) {
        // TODO
        return null;
    }

    /**
     * Deletes a stored conversation from the storage system.
     * 
     * @param conversation The conversation to delete.
     * @return True if the conversation is successfully deleted, false otherwise.
     */
    public boolean deleteConversation(Conversation conversation) {
        // TODO
        return false;
    }

    /**
     * Deletes a specific message from the storage system.
     * 
     * @param message The message to delete.
     * @param conversation The conversation the message belongs to.
     * @return True if the message is successfully deleted, false otherwise.
     */
    public boolean deleteMessage(Message message, Conversation conversation) {
        // TODO
        return false;
    }

    /**
     * Loads all stored conversations from persistent storage into memory.
     * 
     * @return A list of all stored conversations.
     */
    public List<Conversation> loadAllConversations() {
        // TODO
        return null;
    }

    /**
     * Clears all stored data, including messages and conversations.
     * 
     * @return True if all data is successfully cleared, false otherwise.
     */
    public boolean clearAllData() {
        // TODO
        return false;
    }

    /**
     * Updates the details of a stored conversation (e.g., participants or message updates).
     * 
     * @param conversation The conversation to update.
     * @return True if the conversation is successfully updated, false otherwise.
     */
    public boolean updateConversation(Conversation conversation) {
        // TODO
        return false;
    }

    /**
     * Checks if a conversation exists between two users in storage.
     * 
     * @param user1 The first participant.
     * @param user2 The second participant.
     * @return True if the conversation exists, false otherwise.
     */
    public boolean conversationExists(User user1, User user2) {
        // TODO
        return false;
    }
}
