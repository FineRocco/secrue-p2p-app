package com.psd.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    // Unique identifier for the group
    private final String groupId;

    // List of members in the group
    private final List<User> members;

    // Map of all messages sent to the group, with messageID as the key and MessageGroup as the value
    private final Map<String, MessageGroup> messages;

    /**
     * Constructs a new {@code Group} with a specified topic and members.
     *
     * @param topic   The topic of interest for the group conversation.
     * @param members A list of {@link User} instances representing group members.
     * @param groupKey The encryption key shared among members.
     */
    public Group(String groupId, List<User> members) {
        this.groupId = groupId;
        this.members = new ArrayList<>(members);
        this.messages = new HashMap<>();
    }

    /**
     * Gets the unique identifier for this group.
     *
     * @return the groupID.
     */
    public String getGroupID() {
        return groupId;
    }

    /**
     * Gets the list of members in the group.
     *
     * @return the members.
     */
    public List<User> getMembers() {
        return new ArrayList<>(members);
    }

    /**
     * Adds a new member to the group.
     *
     * @param user the {@link User} to add to the group.
     */
    public void addMember(User user) {
        members.add(user);
    }

    /**
     * Removes a member from the group.
     *
     * @param user the {@link User} to remove from the group.
     */
    public void removeMember(User user) {
        members.remove(user);
    }

    /**
     * Adds a message to the group's message map.
     *
     * @param messageGroup The {@link MessageGroup} to add to the group messages.
     */
    public void addMessage(MessageGroup messageGroup) {
        messages.put(messageGroup.getMessageID(), messageGroup);
    }

    /**
     * Returns the map of all messages sent to the group.
     *
     * @return A map where the key is the messageID and the value is the {@link MessageGroup} instance.
     */
    public Map<String, MessageGroup> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return "Group{" +
                "groupID='" + groupId + '\'' +
                ", members=" + members +
                '}';
    }

    public boolean isMember(String userID) {
        for (User member : members) {
            if (member.getUserID().equals(userID)) {
                return true;
            }
        }
        return false;
    }
}
