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
import java.util.List;

public class AWS3Storage {
    private static AWS3Storage instance;
    private final AmazonS3 s3Client;
    private final String conversationBucketName = "psdconversations";
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

    // ------------------ Conversation Methods ------------------ //

    public void saveConversation(String conversationKey, Conversation conversation) throws IOException {
        byte[] conversationBytes = SerializationService.serialize(conversation);
        InputStream inputStream = new ByteArrayInputStream(conversationBytes);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(conversationBytes.length);

        s3Client.putObject(conversationBucketName, conversationKey, inputStream, metadata);
    }

    public Conversation loadConversation(String conversationKey) throws IOException {
        if (!s3Client.doesObjectExist(conversationBucketName, conversationKey)) {
            return null;
        }

        S3Object s3Object = s3Client.getObject(conversationBucketName, conversationKey);
        try (InputStream inputStream = s3Object.getObjectContent()) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }

            byte[] conversationBytes = byteArrayOutputStream.toByteArray();
            return (Conversation) SerializationService.deserialize(conversationBytes);
        }
    }

    public List<String> listAllConversationKeys() {
        List<String> keys = new ArrayList<>();
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(conversationBucketName);
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

    public List<Conversation> getAllConversations(User user) {
        List<Conversation> userConversations = new ArrayList<>();
        List<String> allIds = listAllConversationKeys();

        for (String key : allIds) {
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
        s3Client.deleteObject(conversationBucketName, conversationKey);
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
