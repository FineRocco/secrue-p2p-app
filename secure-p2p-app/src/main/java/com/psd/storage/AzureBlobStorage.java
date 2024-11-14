package com.psd.storage;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
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
    private final BlobContainerClient conversationContainerClient;
    private final BlobContainerClient groupContainerClient;

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

        // Initialize BlobContainerClient for conversations
        this.conversationContainerClient = new BlobContainerClientBuilder()
                .endpoint("https://psd2024.blob.core.windows.net/")
                .containerName("conversations")
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

    // ------------------ Conversation Methods ------------------ //

    public void saveConversation(String conversationKey, Conversation conversation) throws IOException {
        byte[] conversationBytes = SerializationService.serialize(conversation);
        InputStream inputStream = new ByteArrayInputStream(conversationBytes);

        BlobClient blobClient = conversationContainerClient.getBlobClient(conversationKey);
        blobClient.upload(inputStream, conversationBytes.length, true);
    }

    public Conversation loadConversation(String conversationKey) throws IOException {
        BlobClient blobClient = conversationContainerClient.getBlobClient(conversationKey);
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

            return (Conversation) SerializationService.deserialize(outputStream.toByteArray());
        }
    }

    public List<String> listAllConversationKeys() {
        List<String> keys = new ArrayList<>();
        for (BlobItem blobItem : conversationContainerClient.listBlobs()) {
            keys.add(blobItem.getName());
        }
        return keys;
    }

    public List<Conversation> getAllConversations(User user) {
        List<Conversation> userConversations = new ArrayList<>();
        List<String> allKeys = listAllConversationKeys();

        for (String key : allKeys) {
            try {
                Conversation conversation = loadConversation(key);
                if (conversation != null && conversation.isParticipant(user)) {
                    userConversations.add(conversation);
                }
            } catch (IOException e) {
                System.err.println("Error loading conversation for key " + key + ": " + e.getMessage());
            }
        }
        return userConversations;
    }

    public void deleteConversation(String conversationKey) {
        BlobClient blobClient = conversationContainerClient.getBlobClient(conversationKey);
        blobClient.delete();
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
