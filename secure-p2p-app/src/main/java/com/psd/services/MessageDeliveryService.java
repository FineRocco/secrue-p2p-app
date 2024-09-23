package com.psd.services;

import com.psd.entities.*;
import java.util.List;

/**
 * Represents a service responsible for handling the delivery, ordering, 
 * and reliability of messages in the P2P messaging app.
 */
public class MessageDeliveryService {

    // List of all conversations for the system (could be replaced with a more scalable storage solution)
    private List<Conversation> conversations;

    /**
     * Sends a message from the sender to the receiver.
     * Ensures that the message is encrypted and delivered securely.
     * 
     * @param message The message to be sent.
     * @param sender The user sending the message.
     * @param receiver The user receiving the message.
     * @return True if the message is delivered successfully, false otherwise.
     */
    public boolean sendMessage(Message message, User sender, User receiver) {
        // TODO
        return false;
    }

    /**
     * Delivers a message to the receiver.
     * Responsible for ensuring message delivery guarantees such as ordering and reliability.
     * 
     * @param message The message to deliver.
     * @return True if the message is delivered successfully, false otherwise.
     */
    public boolean deliverMessage(Message message) {
        // TODO
        return false;
    }

    /**
     * Retrieves the messages for a specific conversation between two users.
     * 
     * @param user1 The first participant of the conversation.
     * @param user2 The second participant of the conversation.
     * @return A list of messages exchanged between the two users.
     */
    public List<Message> getMessagesBetweenUsers(User user1, User user2) {
        // TODO
        return null;
    }

    /**
     * Ensures message ordering, making sure that messages are delivered in the correct sequence.
     * This can involve checking timestamps or sequence numbers.
     * 
     * @param messageList A list of messages to check for order.
     * @return True if the messages are in the correct order, false otherwise.
     */
    public boolean ensureMessageOrder(List<Message> messageList) {
        // TODO
        return false;
    }

    /**
     * Checks if all messages have been successfully delivered.
     * If not, the service will attempt to retransmit undelivered messages.
     * 
     * @param message The message to check delivery status.
     * @return True if all messages have been delivered, false otherwise.
     */
    public boolean ensureMessageDelivery(Message message) {
        // TODO
        return false;
    }

    /**
     * Resends a message that failed to be delivered.
     * 
     * @param message The message that needs to be resent.
     * @return True if the message is resent successfully, false otherwise.
     */
    public boolean resendMessage(Message message) {
        // TODO
        return false;
    }

    /**
     * Adds a conversation between two users to the list of conversations.
     * 
     * @param conversation The conversation to add.
     */
    public void addConversation(Conversation conversation) {
        // TODO
    }

    /**
     * Retrieves the conversation between two users, or creates a new conversation if one doesn't exist.
     * 
     * @param user1 The first participant.
     * @param user2 The second participant.
     * @return The existing or newly created conversation.
     */
    public Conversation getOrCreateConversation(User user1, User user2) {
        // TODO
        return null;
    }
}
