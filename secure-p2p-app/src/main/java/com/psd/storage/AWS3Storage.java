package com.psd.storage;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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

public class AWS3Storage {
    private static AWS3Storage instance;
    private final AmazonS3 s3Client;
    private final String groupBucketName = "psdgroups";

    /**
     * Constructor to initialize AWS S3 client with the specified credentials.
     */
    private AWS3Storage() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIA5MSUBYPZP7FNUJMD", "EshRtv/7PUHzEDpf8/7Mo5lU1aGORC8r2/a4PWMV");
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.EU_NORTH_1)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }

    // Singleton instance accessor
    public static AWS3Storage getInstance() {
        if (instance == null) {
            synchronized (AWS3Storage.class) {
                if (instance == null) {
                    instance = new AWS3Storage();
                }
            }
        }
        return instance;
    }

    /**
     * Creates an S3 bucket with the given user ID as the bucket name.
     *
     * @param userId The user ID to use as the bucket name.
     * @throws IllegalArgumentException if the bucket name is invalid.
     */
    public void createBucketForUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty.");
        }

        String bucketName = "psd-" + userId.toLowerCase();

        if (s3Client.doesBucketExistV2(bucketName)) {
            System.out.println("Bucket already exists: " + bucketName);
            return;
        }

        s3Client.createBucket(bucketName);
        System.out.println("Bucket created successfully: " + bucketName);
    }

    /**
     * Saves a share of the key into the user's bucket.
     *
     * @param userId The ID of the user whose bucket the share will be saved into.
     * @param shareId The unique identifier for the share.
     * @param shareData The share data as a Base64-encoded string.
     */
    public void saveKeyShare(String userId, String shareId, String shareData) {
        try {
            // Create the user-specific bucket if it doesn't exist
            createBucketForUser(userId);

            // Prepare the data for upload
            byte[] shareBytes = shareData.getBytes();
            InputStream inputStream = new ByteArrayInputStream(shareBytes);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(shareBytes.length);
            metadata.setContentType("text/plain");

            // Save the share to the user's bucket
            String bucketName = "psd-" + userId.toLowerCase();
            String objectKey = "shares/" + shareId; // Store shares in a 'shares' folder within the bucket
            s3Client.putObject(bucketName, objectKey, inputStream, metadata);

            System.out.println("Key share saved successfully: " + objectKey + " in bucket: " + bucketName);
        } catch (Exception e) {
            System.err.println("Error saving key share: " + e.getMessage());
        }
    }

    /**
     * Checks if a share exists in the specified user's bucket.
     *
     * @param userId The ID of the user whose bucket will be checked.
     * @param shareId The unique identifier for the share.
     * @return {@code true} if the share exists, {@code false} otherwise.
     */
    public boolean checkShareExists(String userId, String shareId) {
        try {
            String bucketName = "psd-" + userId.toLowerCase();
            String objectKey = "shares/" + shareId; // Look for shares in the 'shares' folder

            // Check if the object exists in the bucket
            boolean exists = s3Client.doesObjectExist(bucketName, objectKey);
            System.out.println("Checked share existence: " + objectKey + " in bucket: " + bucketName + " - Exists: " + exists);
            return exists;
        } catch (Exception e) {
            System.err.println("Error checking share existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads a share of the key from the specified user's bucket.
     *
     * @param userId The ID of the user whose bucket the share will be loaded from.
     * @param shareId The unique identifier for the share.
     * @return The share data as a Base64-encoded string, or {@code null} if the share does not exist.
     * @throws IOException If an error occurs during the loading process.
     */
    public String loadKeyShare(String userId, String shareId) throws IOException {
        try {
            String bucketName = "psd-" + userId.toLowerCase();
            String objectKey = "shares/" + shareId; // Shares are stored in the 'shares' folder

            // Check if the object exists
            if (!s3Client.doesObjectExist(bucketName, objectKey)) {
                System.out.println("Share not found: " + objectKey + " in bucket: " + bucketName);
                return null;
            }

            // Retrieve the object from the bucket
            S3Object s3Object = s3Client.getObject(bucketName, objectKey);

            // Read the object's content into a string
            try (InputStream inputStream = s3Object.getObjectContent()) {
                return new String(inputStream.readAllBytes());
            }
        } catch (Exception e) {
            System.err.println("Error loading key share: " + e.getMessage());
            throw new IOException("Error loading key share", e);
        }
    }

    // ------------------ Encrypted Conversation Methods ------------------ //

    public void saveEncryptedConversation(String conversationKey, String encryptedConversation, String userId) throws IOException {
        try {
            // Convert the encrypted conversation string to bytes
            byte[] encryptedBytes = encryptedConversation.getBytes();
            InputStream inputStream = new ByteArrayInputStream(encryptedBytes);

            // Set metadata for the object
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(encryptedBytes.length);
            metadata.setContentType("text/plain");

            // Save the encrypted conversation in the user's bucket
            String bucketName = "psd-" + userId.toLowerCase();
            s3Client.putObject(bucketName, conversationKey, inputStream, metadata);

            System.out.println("Encrypted conversation saved successfully: " + conversationKey);
        } catch (Exception e) {
            System.err.println("Error saving encrypted conversation: " + e.getMessage());
            throw new IOException("Error saving encrypted conversation", e);
        }
    }

    public String loadEncryptedConversation(String conversationKey, String userId) throws IOException {
        String bucketName = "psd-" + userId.toLowerCase();
        if (!s3Client.doesObjectExist(bucketName, conversationKey)) {
            System.out.println("Encrypted conversation not found for key: " + conversationKey);
            return null;
        }

        S3Object s3Object = s3Client.getObject(bucketName, conversationKey);
        try (InputStream inputStream = s3Object.getObjectContent();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            // Return the encrypted conversation as a String
            return outputStream.toString();
        } catch (Exception e) {
            System.err.println("Error loading encrypted conversation: " + e.getMessage());
            throw new IOException("Error loading encrypted conversation", e);
        }
    }

    public Map<String, String> listAllEncryptedConversations(User user) {
        Map<String, String> encryptedConversations = new HashMap<>();
        String bucketName = "psd-" + user.getUserID().toLowerCase();
    
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result result;
    
        do {
            result = s3Client.listObjectsV2(req);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                String conversationId = objectSummary.getKey();
    
                // Skip objects in the "shares" folder
                if (conversationId.startsWith("shares/")) {
                    continue;
                }
    
                try {
                    // Retrieve the object content (encrypted data)
                    S3Object s3Object = s3Client.getObject(bucketName, conversationId);
                    try (InputStream inputStream = s3Object.getObjectContent();
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
            req.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());
    
        return encryptedConversations;
    }    

    public void deleteEncryptedConversation(String conversationKey, String userId) {
        String bucketName = "psd-" + userId.toLowerCase();
        try {
            s3Client.deleteObject(bucketName, conversationKey);
            System.out.println("Deleted encrypted conversation: " + conversationKey);
        } catch (Exception e) {
            System.err.println("Error deleting encrypted conversation: " + e.getMessage());
        }
    }

    // ------------------ Group Methods ------------------ //

    public void saveGroup(String groupId, Group group) throws IOException {
        byte[] groupBytes = SerializationService.serialize(group);
        InputStream inputStream = new ByteArrayInputStream(groupBytes);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(groupBytes.length);

        s3Client.putObject(groupBucketName, groupId, inputStream, metadata);
    }

    public Group loadGroup(String groupId) throws IOException {
        if (!s3Client.doesObjectExist(groupBucketName, groupId)) {
            return null;
        }

        S3Object s3Object = s3Client.getObject(groupBucketName, groupId);
        try (InputStream inputStream = s3Object.getObjectContent()) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }

            byte[] groupBytes = byteArrayOutputStream.toByteArray();
            return (Group) SerializationService.deserialize(groupBytes);
        }
    }


    public List<String> listAllgroupIds() {
        List<String> keys = new ArrayList<>();
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(groupBucketName);
        ListObjectsV2Result result;

        do {
            result = s3Client.listObjectsV2(req);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                keys.add(objectSummary.getKey());
            }
            req.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return keys;
    }

    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        List<String> allIds = listAllgroupIds();

        for (String key : allIds) {
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

    /**
     * Retrieves all groups where the specified user is a member.
     *
     * @param currentUser The user whose group memberships should be checked.
     * @return A list of Group objects where the specified user is a member.
     */
    public List<Group> getAllGroupsWithMember(User currentUser) {
        List<Group> userGroups = new ArrayList<>();
        List<String> allIds = listAllgroupIds();
        System.out.println("All group ids: " + allIds);
    
        for (String id : allIds) {
            try {
                Group group = loadGroup(id);
                if (group != null && group.getMembers().contains(currentUser)) {
                    userGroups.add(group);
                }
            } catch (IOException e) {
                System.err.println("Error loading group for key " + id + ": " + e.getMessage());
            }
        }
        return userGroups;
    }

    public void deleteGroup(String groupId) {
        s3Client.deleteObject(groupBucketName, groupId);
    }
}
