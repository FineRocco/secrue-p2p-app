package com.psd.storage;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.psd.entities.Conversation;
import com.psd.entities.User;
import com.psd.services.SerializationService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AzureBlobStorage {
    private final BlobContainerClient containerClient;
    private final String containerName = "conversations";

    /**
     * Constructor to initialize Azure Blob Storage client with the specified container name.
     *
     * @param containerName Azure Blob Storage container name where conversations will be stored.
     */
    public AzureBlobStorage() {
        // Set up ClientSecretCredential directly
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId("25893d58-56d2-4bff-9ef6-eabe72bcd772")
                .clientSecret("s-m8Q~LFQJunMYtlIWADmPt1FYfDROL0z7l4OaED")
                .tenantId("0bfa8500-b1f2-4566-baf1-6f59370893e7")
                .build();
        // Initialize BlobContainerClient with DefaultAzureCredential
        this.containerClient = new BlobContainerClientBuilder()
                .endpoint("https://psd2024.blob.core.windows.net/")
                .containerName(containerName)
                .credential(clientSecretCredential)
                .buildClient();
    }

    /**
     * Saves a conversation to the Blob container.
     *
     * @param conversationKey The unique key for the conversation.
     * @param conversation The Conversation object to save.
     * @throws IOException If an error occurs during serialization.
     */
    public void saveConversation(String conversationKey, Conversation conversation) throws IOException {
        byte[] conversationBytes = SerializationService.serialize(conversation);
        InputStream inputStream = new ByteArrayInputStream(conversationBytes);

        // Create BlobClient for the specific conversation
        System.out.println("Gettint blobClient from container: " + containerName);
        BlobClient blobClient = containerClient.getBlobClient(conversationKey);
        System.out.println("BlobClient created: " + blobClient.getBlobUrl() + " & " + blobClient.getContainerName());
        System.out.println("Uploading conversation to Azure Blob: " + conversationKey);
        blobClient.upload(inputStream, conversationBytes.length, true); // Overwrite if exists
        System.out.println("Uploaded conversation to Azure Blob: " + conversationKey);
    }

    /**
     * Loads a conversation from the Blob container.
     *
     * @param conversationKey The unique key for the conversation.
     * @return The Conversation object retrieved from Blob, or null if not found.
     * @throws IOException If an error occurs during deserialization.
     */
    public Conversation loadConversation(String conversationKey) throws IOException {
        BlobClient blobClient = containerClient.getBlobClient(conversationKey);
        if (!blobClient.exists()) {
            return null; // Conversation does not exist
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
     * Lists all conversation keys (blob names) in the Blob container.
     *
     * @return A list of keys representing all conversations in the container.
     */
    public List<String> listAllConversationKeys() {
        List<String> keys = new ArrayList<>();
        for (BlobItem blobItem : containerClient.listBlobs()) {
            keys.add(blobItem.getName());
        }
        return keys;
    }

    /**
     * Retrieves all conversations for a specified user.
     *
     * @param user The user whose conversations are to be retrieved.
     * @return A list of Conversation objects involving the user.
     */
    public List<Conversation> getAllConversations(User user) {
        List<Conversation> userConversations = new ArrayList<>();
        List<String> allKeys = listAllConversationKeys();

        System.out.println("Retrieved all conversation keys from Azure Blob: " + allKeys);

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

    /**
     * Deletes a conversation from the Blob container.
     *
     * @param conversationKey The unique key for the conversation.
     */
    public void deleteConversation(String conversationKey) {
        BlobClient blobClient = containerClient.getBlobClient(conversationKey);
        blobClient.delete();
    }
}
