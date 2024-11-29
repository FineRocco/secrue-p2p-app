package com.psd.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psd.entities.Conversation;
import com.psd.entities.Group;
import com.psd.entities.User;
import com.psd.services.SerializationService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Checks if the `dictionaries` folder exists in the user's Azure Blob container.
     *
     * @param userId The ID of the user whose container will be checked.
     * @return {@code true} if the `dictionaries` folder exists, {@code false} otherwise.
     */
    public boolean checkDicExists(String userId) {
        try {
            // Get the user's container
            BlobContainerClient userContainerClient = getUserContainerClient(userId);

            // Check if any blob exists under the `dictionaries/` prefix
            PagedIterable<BlobItem> blobs = userContainerClient.listBlobsByHierarchy("dictionaries/");
            if (blobs.iterator().hasNext()) {
                System.out.println("Dictionaries folder exists for user: " + userId);
                return true; // Folder exists
            }

            System.out.println("Dictionaries folder does not exist for user: " + userId);
            return false;
        } catch (Exception e) {
            System.err.println("Error checking dictionaries folder existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates the `dictionaries` folder in the user's Azure Blob container.
     *
     * @param userId The ID of the user whose container will be updated.
     */
    public void createDictionariesFolder(String userId) {
        try {
            // Get the user's container
            BlobContainerClient userContainerClient = getUserContainerClient(userId);

            // Create a dummy blob to initialize the `dictionaries` folder
            BlobClient blobClient = userContainerClient.getBlobClient("dictionaries/init");
            byte[] dummyData = "init".getBytes();
            InputStream inputStream = new ByteArrayInputStream(dummyData);

            blobClient.upload(inputStream, dummyData.length, true);
            System.out.println("Created dictionaries folder in Azure Blob Storage for user: " + userId);
        } catch (Exception e) {
            System.err.println("Error creating dictionaries folder: " + e.getMessage());
        }
    }

    // ------------------ Dictionaire Methods ------------------ //

    /**
     * Saves an encrypted word and its metadata to the `dictionaries` folder in the user's Azure Blob container.
     *
     * @param userId          The ID of the user whose container will be updated.
     * @param encryptedWord   The encrypted word to save.
     * @param encryptedMetadata The encrypted metadata associated with the word.
     */
    public void saveWordToDic(String userId, String encryptedWord, String encryptedMetadata) {
        try {
            // Get the user's container
            BlobContainerClient userContainerClient = getUserContainerClient(userId);

            // Construct the blob path for the encrypted word
            BlobClient blobClient = userContainerClient.getBlobClient("dictionaries/" + encryptedWord);

            // Prepare the metadata data as bytes
            byte[] metadataBytes = encryptedMetadata.getBytes();
            InputStream inputStream = new ByteArrayInputStream(metadataBytes);

            // Check if the word already exists
            if (blobClient.exists()) {
                System.out.println("Word already exists in dictionary for user " + userId + ": " + encryptedWord);
            } else {
                // Upload the metadata
                blobClient.upload(inputStream, metadataBytes.length, true);
                System.out.println("Saved word to Azure Blob Storage dictionary for user: " + userId);
            }
        } catch (Exception e) {
            System.err.println("Error saving word to Azure Blob Storage dictionary: " + e.getMessage());
        }
    }

    /**
     * Checks if a specific word exists in the `dictionaries` folder of the user's Azure Blob container.
     *
     * @param userId        The ID of the user whose container will be checked.
     * @param encryptedWord The encrypted word to check for existence.
     * @return {@code true} if the word exists, {@code false} otherwise.
     */
    public boolean checkWordExistsInDic(String userId, String encryptedWord) {
        try {
            // Get the user's container
            BlobContainerClient userContainerClient = getUserContainerClient(userId);

            // Get the blob client for the specific word
            BlobClient blobClient = userContainerClient.getBlobClient("dictionaries/" + encryptedWord);

            // Check if the blob exists
            boolean exists = blobClient.exists();
            System.out.println("Checked existence of word '" + encryptedWord + "' for user " + userId + ": " + exists);
            return exists;
        } catch (Exception e) {
            System.err.println("Error checking word existence in Azure Blob Storage: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads the encrypted metadata for a specified word from the `dictionaries` folder in the user's Azure Blob container.
     *
     * @param userId        The ID of the user whose container will be accessed.
     * @param encryptedWord The encrypted word whose metadata will be loaded.
     * @return The encrypted metadata as a String if the word exists, or {@code null} if it doesn't exist.
     */
    public String loadWordMetadata(String userId, String encryptedWord) {
        try {
            // Get the user's container
            BlobContainerClient userContainerClient = getUserContainerClient(userId);

            // Construct the path to the encrypted word blob
            BlobClient blobClient = userContainerClient.getBlobClient("dictionaries/" + encryptedWord);

            // Check if the word exists
            if (!blobClient.exists()) {
                System.out.println("Word not found in dictionary for user " + userId + ": " + encryptedWord);
                return null;
            }

            // Read the blob's content and return it as a string
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                blobClient.downloadStream(outputStream);
                return outputStream.toString(); // Return the encrypted metadata as a string
            }
        } catch (Exception e) {
            System.err.println("Error loading word metadata from Azure Blob Storage dictionary: " + e.getMessage());
            return null;
        }
    }

    // ------------------ Encrypted Conversation Methods ------------------ //

    /**
     * Saves an encrypted conversation to a user-specific container.
     *
     * @param conversationKey The unique key for the conversation.
     * @param encryptedConversation The encrypted conversation string to save.
     * @param userId The ID of the user for whom the conversation is saved.
     */
    public void saveEncryptedConversation(String conversationKey, String encryptedConversation, String userId) {
        BlobContainerClient userContainerClient = getUserContainerClient(userId);

        try {
            // Convert the encrypted conversation string to bytes
            byte[] encryptedBytes = encryptedConversation.getBytes();
            InputStream inputStream = new ByteArrayInputStream(encryptedBytes);

            // Upload the encrypted conversation to Azure Blob Storage
            BlobClient blobClient = userContainerClient.getBlobClient(conversationKey);
            blobClient.upload(inputStream, encryptedBytes.length, true);

            System.out.println("Saved encrypted conversation with key " + conversationKey + " in container for user " + userId);
        } catch (Exception e) {
            System.err.println("Error saving encrypted conversation: " + e.getMessage());
            throw new RuntimeException("Error saving encrypted conversation", e);
        }
    }

    /**
     * Loads an encrypted conversation from a user-specific container.
     *
     * @param conversationKey The unique key for the conversation.
     * @param userId The ID of the user for whom the conversation is loaded.
     * @return The encrypted conversation as a string, or null if not found.
     */
    public String loadEncryptedConversation(String conversationKey, String userId) {
        BlobContainerClient userContainerClient = getUserContainerClient(userId);

        BlobClient blobClient = userContainerClient.getBlobClient(conversationKey);
        if (!blobClient.exists()) {
            System.out.println("Encrypted conversation with key " + conversationKey + " not found for user " + userId);
            return null;
        }

        try (InputStream inputStream = blobClient.openInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            // Return the encrypted conversation as a string
            return outputStream.toString();
        } catch (Exception e) {
            System.err.println("Error loading encrypted conversation: " + e.getMessage());
            throw new RuntimeException("Error loading encrypted conversation", e);
        }
    }

    /**
     * Lists all encrypted conversations (ID and encrypted data) in a user-specific container.
     *
     * @param userId The ID of the user whose conversations are listed.
     * @return A map where the key is the conversation ID and the value is the encrypted data as a string.
     */
    public Map<String, String> listAllEncryptedConversations(String userId) {
        BlobContainerClient userContainerClient = getUserContainerClient(userId);

        Map<String, String> encryptedConversations = new HashMap<>();
        for (BlobItem blobItem : userContainerClient.listBlobs()) {
            String conversationId = blobItem.getName();

            // Skip blobs in the "shares" folder
            if (conversationId.startsWith("shares/")) {
                continue;
            }
            // Skip blobs in the "shares" folder
            if (conversationId.startsWith("dictionaries/")) {
                continue;
            }

            try {
                // Retrieve the content of the blob (encrypted data)
                BlobClient blobClient = userContainerClient.getBlobClient(conversationId);
                try (InputStream inputStream = blobClient.openInputStream();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, length);
                    }
                    // Convert the data to a string (assuming Base64-encoded encrypted data)
                    String encryptedData = byteArrayOutputStream.toString();
                    encryptedConversations.put(conversationId, encryptedData);
                }
            } catch (Exception e) {
                System.err.println("Error retrieving encrypted conversation for key " + conversationId + ": " + e.getMessage());
            }
        }

        return encryptedConversations;
    }

    /**
     * Deletes an encrypted conversation from a user-specific container.
     *
     * @param conversationKey The unique key for the conversation.
     * @param userId The ID of the user for whom the conversation is deleted.
     */
    public void deleteEncryptedConversation(String conversationKey, String userId) {
        BlobContainerClient userContainerClient = getUserContainerClient(userId);

        BlobClient blobClient = userContainerClient.getBlobClient(conversationKey);
        if (blobClient.exists()) {
            blobClient.delete();
            System.out.println("Deleted encrypted conversation with key " + conversationKey + " for user " + userId);
        } else {
            System.out.println("Encrypted conversation with key " + conversationKey + " does not exist for user " + userId);
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
