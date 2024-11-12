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
import com.psd.entities.User;
import com.psd.services.SerializationService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AWS3Storage {
    private final AmazonS3 s3Client;
    private final String bucketName = "myawsbucketpsd";

    /**
     * Constructor to initialize AWS S3 client with the specified credentials and bucket name.
     *
     * @param accessKey AWS access key.
     * @param secretKey AWS secret key.
     * @param bucketName S3 bucket name where conversations will be stored.
     */
    public AWS3Storage() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIA5MSUBYPZP7FNUJMD", "EshRtv/7PUHzEDpf8/7Mo5lU1aGORC8r2/a4PWMV");
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.EU_NORTH_1) // Specify your AWS region
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }

    /**
     * Saves a conversation to the S3 bucket.
     *
     * @param conversationKey The unique key for the conversation.
     * @param conversation The Conversation object to save.
     * @throws IOException If an error occurs during serialization.
     */
    public void saveConversation(String conversationKey, Conversation conversation) throws IOException {
        // Serialize the conversation
        byte[] conversationBytes = SerializationService.serialize(conversation);
        
        // Convert bytes to an InputStream
        InputStream inputStream = new ByteArrayInputStream(conversationBytes);
        
        // Create object metadata
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(conversationBytes.length);

        // Save conversation in S3
        s3Client.putObject(bucketName, conversationKey, inputStream, metadata);
    }

    /**
     * Loads a conversation from the S3 bucket.
     *
     * @param conversationKey The unique key for the conversation.
     * @return The Conversation object retrieved from S3, or null if not found.
     * @throws IOException If an error occurs during deserialization.
     */
    public Conversation loadConversation(String conversationKey) throws IOException {
        if (!s3Client.doesObjectExist(bucketName, conversationKey)) {
            return null; // Conversation does not exist
        }
        
        S3Object s3Object = s3Client.getObject(bucketName, conversationKey);
        try (InputStream inputStream = s3Object.getObjectContent()) {
            // Read the content as a byte array
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            
            // Deserialize the conversation
            byte[] conversationBytes = byteArrayOutputStream.toByteArray();
            return (Conversation) SerializationService.deserialize(conversationBytes);
        }
    }

    /**
     * Lists all conversation keys (object keys) in the S3 bucket.
     *
     * @return A list of keys representing all conversations in the bucket.
     */
    public List<String> listAllConversationKeys() {
        List<String> keys = new ArrayList<>();
        
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result result;
        
        do {
            result = s3Client.listObjectsV2(req);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                keys.add(objectSummary.getKey());
            }
            // Fetch the next batch if there are more objects
            req.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated()); // Continues if the bucket has more than 1000 objects
        
        return keys;
    }

    public List<Conversation> getAllConversations(User user) {
        List<Conversation> userConversations = new ArrayList<>();
        List<String> allKeys = listAllConversationKeys(); // Get all stored keys

        System.out.println("Retrieved all conversation keys from S3: " + allKeys); // Log keys

        for (String key : allKeys) {
            try {
                Conversation conversation = loadConversation(key);
                if (conversation != null) {
                    System.out.println("Loaded conversation for key: " + key + " with participants: " +
                            conversation.getParticipant1().getUserID() + " and " + conversation.getParticipant2().getUserID());

                    if (conversation.isParticipant(user)) {
                        System.out.println("Adding conversation for user: " + user.getUserID());
                        userConversations.add(conversation);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading conversation for key " + key + ": " + e.getMessage());
            }
        }
        return userConversations;
    }

    /**
     * Deletes a conversation from the S3 bucket.
     *
     * @param conversationKey The unique key for the conversation.
     */
    public void deleteConversation(String conversationKey) {
        s3Client.deleteObject(bucketName, conversationKey);
    }

}
