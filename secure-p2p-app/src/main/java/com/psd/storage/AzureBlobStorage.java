package com.psd.storage;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.psd.entities.Conversation;
import com.psd.entities.Group;
import com.psd.entities.User;
import com.psd.services.SerializationService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AzureBlobStorage {
    private static AzureBlobStorage instance;
    private final BlobContainerClient groupContainerClient;
    private final BlobServiceClient blobServiceClient;

    /**
     * Constructor to initialize Azure Blob Storage clients for both containers.
     */
    public AzureBlobStorage() {
        // Set up ClientSecretCredential
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId("25893d58-56d2-4bff-9ef6-eabe72bcd772")
                .clientSecret("s-m8Q~LFQJunMYtlIWADmPt1FYfDROL0z7l4OaED")
                .tenantId("0bfa8500-b1f2-4566-baf1-6f59370893e7")
                .build();

        // Initialize BlobServiceClient
        this.blobServiceClient = new BlobServiceClientBuilder()
                .endpoint("https://psd2024.blob.core.windows.net/")
                .credential(clientSecretCredential)
                .buildClient();

        // Initialize BlobContainerClient for groups
        this.groupContainerClient = new BlobContainerClientBuilder()
                .endpoint("https://psd2024.blob.core.windows.net/")
                .containerName("groups")
                .credential(clientSecretCredential)
                .buildClient();
    }

    // Public method to provide access to the singleton instance
    public static synchronized AzureBlobStorage getInstance() {
        if (instance == null) {
            instance = new AzureBlobStorage();
        }
        return instance;
    }

    /**
     * Ensures a user-specific container exists.
     *
     * @param userId The ID of the user for whom the container is created.
     * @return The BlobContainerClient for the user's container.
     */
    public BlobContainerClient getUserContainerClient(String userId) {
        String containerName = "user-" + userId.toLowerCase();
        BlobContainerClient userContainerClient = blobServiceClient.getBlobContainerClient(containerName);

        if (!userContainerClient.exists()) {
            userContainerClient.create();
            System.out.println("Created container: " + containerName);
        }

        return userContainerClient;
    }

    // ------------------ Key Share Methods ------------------ //

    /**
     * Saves a share of the key into the user's Azure Blob container.
     *
     * @param userId   The ID of the user whose container the share will be saved into.
     * @param shareId  The unique identifier for the share.
     * @param shareData The share data as a Base64-encoded string.
     */
    public void saveKeyShare(String userId, String shareId, String shareData) {
        try {
            // Get or create the user's container
            BlobContainerClient userContainerClient = getUserContainerClient(userId);

            // Save the share data to a blob
            BlobClient blobClient = userContainerClient.getBlobClient("shares/" + shareId); // Store shares in a "shares" folder
            byte[] shareBytes = shareData.getBytes();
            InputStream inputStream = new ByteArrayInputStream(shareBytes);

            blobClient.upload(inputStream, shareBytes.length, true);
            System.out.println("Key share saved successfully: " + shareId + " for user: " + userId);
        } catch (Exception e) {
            System.err.println("Error saving key share: " + e.getMessage());
        }
    }

    /**
     * Loads a share of the key from the user's Azure Blob container.
     *
     * @param userId  The ID of the user whose container the share will be loaded from.
     * @param shareId The unique identifier for the share.
     * @return The share data as a Base64-encoded string, or {@code null} if the share does not exist.
     */
    public String loadKeyShare(String userId, String shareId) {
        try {
            // Get the user's container
            BlobContainerClient userContainerClient = getUserContainerClient(userId);

            // Retrieve the blob containing the share data
            BlobClient blobClient = userContainerClient.getBlobClient("shares/" + shareId);

            if (!blobClient.exists()) {
                System.out.println("Share not found: " + shareId + " for user: " + userId);
                return null;
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                blobClient.downloadStream(outputStream);
                String shareData = new String(outputStream.toByteArray());
                System.out.println("Loaded key share: " + shareId + " for user: " + userId);
                return shareData;
            }
        } catch (Exception e) {
            System.err.println("Error loading key share: " + e.getMessage());
            return null;
        }
    }


    /**
     * Checks if a share exists in the user's Azure Blob container.
     *
     * @param userId  The ID of the user whose container will be checked.
     * @param shareId The unique identifier for the share.
     * @return {@code true} if the share exists, {@code false} otherwise.
     */
    public boolean checkShareExists(String userId, String shareId) {
        try {
            // Get the user's container
            BlobContainerClient userContainerClient = getUserContainerClient(userId);

            // Check if the blob exists
            BlobClient blobClient = userContainerClient.getBlobClient("shares/" + shareId);
            boolean exists = blobClient.exists();

            System.out.println("Checked share existence: " + shareId + " for user: " + userId + " - Exists: " + exists);
            return exists;
        } catch (Exception e) {
            System.err.println("Error checking share existence: " + e.getMessage());
            return false;
        }
    }

    // ------------------ Conversation Methods ------------------ //

    /**
     * Saves a conversation to a user-specific container.
     *
     * @param conversationKey The unique key for the conversation.
     * @param conversation The Conversation object to save.
     * @param userId The ID of the user for whom the conversation is saved.
     * @throws IOException If an error occurs during serialization.
     */
    public void saveConversation(String conversationKey, Conversation conversation, String userId) throws IOException {
        BlobContainerClient userContainerClient = getUserContainerClient(userId);

        byte[] conversationBytes = SerializationService.serialize(conversation);
        InputStream inputStream = new ByteArrayInputStream(conversationBytes);

        BlobClient blobClient = userContainerClient.getBlobClient(conversationKey);
        blobClient.upload(inputStream, conversationBytes.length, true);

        System.out.println("Saved conversation with key " + conversationKey + " in container for user " + userId);
    }

    /**
     * Loads a conversation from a user-specific container.
     *
     * @param conversationKey The unique key for the conversation.
     * @param userId The ID of the user for whom the conversation is loaded.
     * @return The Conversation object, or null if not found.
     * @throws IOException If an error occurs during deserialization.
     */
    public Conversation loadConversation(String conversationKey, String userId) throws IOException {
        BlobContainerClient userContainerClient = getUserContainerClient(userId);

        BlobClient blobClient = userContainerClient.getBlobClient(conversationKey);
        if (!blobClient.exists()) {
            System.out.println("Conversation with key " + conversationKey + " not found for user " + userId);
            return null;
        }

        try (InputStream inputStream = blobClient.openInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            return (Conversation) SerializationService.deserialize(outputStream.toByteArray());
        }
    }

    /**
     * Lists all conversation keys in a user-specific container.
     *
     * @param userId The ID of the user whose conversations are listed.
     * @return A list of keys representing all conversations in the user's container.
     */
    public List<String> listAllConversationKeys(String userId) {
        BlobContainerClient userContainerClient = getUserContainerClient(userId);

        List<String> keys = new ArrayList<>();
        for (BlobItem blobItem : userContainerClient.listBlobs()) {
            keys.add(blobItem.getName());
        }

        return keys;
    }

    /**
     * Retrieves all conversations for a user from their container.
     *
     * @param user The user whose conversations are retrieved.
     * @return A list of Conversation objects for the specified user.
     */
    public List<Conversation> getAllConversations(User user) {
        List<Conversation> userConversations = new ArrayList<>();
        String userId = user.getUserID();
        List<String> allKeys = listAllConversationKeys(userId);

        for (String key : allKeys) {
            try {
                Conversation conversation = loadConversation(key, userId);
                if (conversation != null && conversation.isParticipant(user)) {
                    userConversations.add(conversation);
                }
            } catch (IOException e) {
                System.err.println("Error loading conversation for key " + key + ": " + e.getMessage());
            }
        }
        return userConversations;
    }

    /**
     * Deletes a conversation from a user-specific container.
     *
     * @param conversationKey The unique key for the conversation.
     * @param userId The ID of the user for whom the conversation is deleted.
     */
    public void deleteConversation(String conversationKey, String userId) {
        BlobContainerClient userContainerClient = getUserContainerClient(userId);

        BlobClient blobClient = userContainerClient.getBlobClient(conversationKey);
        if (blobClient.exists()) {
            blobClient.delete();
            System.out.println("Deleted conversation with key " + conversationKey + " for user " + userId);
        } else {
            System.out.println("Conversation with key " + conversationKey + " does not exist for user " + userId);
        }
    }

    // ------------------ Group Methods ------------------ //

    public void saveGroup(String groupKey, Group group) throws IOException {
        byte[] groupBytes = SerializationService.serialize(group);
        InputStream inputStream = new ByteArrayInputStream(groupBytes);

        BlobClient blobClient = groupContainerClient.getBlobClient(groupKey);
        blobClient.upload(inputStream, groupBytes.length, true);
    }

    public Group loadGroup(String groupKey) throws IOException {
        BlobClient blobClient = groupContainerClient.getBlobClient(groupKey);
        if (!blobClient.exists()) {
            return null;
        }

        try (InputStream inputStream = blobClient.openInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            return (Group) SerializationService.deserialize(outputStream.toByteArray());
        }
    }

    /**
     * Retrieves all groups where the specified user is a member.
     *
     * @param currentUser The user whose group memberships should be checked.
     * @return A list of Group objects where the specified user is a member.
     */
    public List<Group> getAllGroupsWithMember(User currentUser) {
        List<Group> userGroups = new ArrayList<>();
        List<String> allKeys = listAllGroupKeys();
    
        for (String key : allKeys) {
            try {
                Group group = loadGroup(key);
                if (group != null && group.getMembers().contains(currentUser)) {
                    userGroups.add(group);
                }
            } catch (IOException e) {
                System.err.println("Error loading group for key " + key + ": " + e.getMessage());
            }
        }
        return userGroups;
    }

    public List<String> listAllGroupKeys() {
        List<String> keys = new ArrayList<>();
        for (BlobItem blobItem : groupContainerClient.listBlobs()) {
            keys.add(blobItem.getName());
        }
        return keys;
    }

    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        List<String> allKeys = listAllGroupKeys();

        for (String key : allKeys) {
            try {
                Group group = loadGroup(key);
                if (group != null) {
                    groups.add(group);
                }
            } catch (IOException e) {
                System.err.println("Error loading group for key " + key + ": " + e.getMessage());
            }
        }
        return groups;
    }

    public void deleteGroup(String groupKey) {
        BlobClient blobClient = groupContainerClient.getBlobClient(groupKey);
        blobClient.delete();
    }
}
