package com.psd.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.psd.entities.Conversation;
import com.psd.entities.Group;
import com.psd.entities.User;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FirebaseStorage {
    private static FirebaseStorage instance;  // Singleton instance
    private Firestore db = null; // Initialize to null by default

    /**
     * Constructor to initialize Firebase Firestore client.
     *
     */
    public FirebaseStorage() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream("resources\\psd2024-da5c7-firebase-adminsdk-bwigq-a0d2f9524d.json");

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase successfully initialized.");
            }
            db = FirestoreClient.getFirestore();
        } catch (IOException e) {
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
        }
    }

    /**
     * Returns the singleton instance of FirebaseStorage.
     *
     * @return FirebaseStorage instance.
     */
    public static synchronized FirebaseStorage getInstance() {
        if (instance == null) {
            instance = new FirebaseStorage();
        }
        return instance;
    }

    /**
     * Creates a user-specific collection in Firestore.
     *
     * @param userId The ID of the user for whom the collection is being created.
     */
    public void createUserCollection(String userId) {
        try {
            String collectionName = "user_" + userId;
            CollectionReference collectionRef = db.collection(collectionName);

            // Verify if the collection exists by attempting to fetch documents
            QuerySnapshot snapshot = collectionRef.limit(1).get().get();
            if (snapshot.isEmpty()) {
                System.out.println("Created collection for user: " + userId);
            } else {
                System.out.println("Collection already exists for user: " + userId);
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error creating user collection: " + e.getMessage());
        }
    }

    // ------------------ Key Share Methods ------------------ //

    /**
     * Saves a share of the key into the user's Firestore collection.
     *
     * @param userId   The ID of the user whose collection the share will be saved into.
     * @param shareId  The unique identifier for the share.
     * @param shareData The share data as a Base64-encoded string.
     */
    public void saveKeyShare(String userId, String shareId, String shareData) {
        try {
            String collectionName = "user_" + userId;
            DocumentReference docRef = db.collection(collectionName).document("shares_" + shareId);

            // Save the share data
            Map<String, Object> share = new HashMap<>();
            share.put("shareId", shareId);
            share.put("data", shareData);
            docRef.set(share).get();

            System.out.println("Key share saved successfully: " + shareId + " for user: " + userId);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error saving key share: " + e.getMessage());
        }
    }

    /**
     * Loads a share of the key from the user's Firestore collection.
     *
     * @param userId  The ID of the user whose collection the share will be loaded from.
     * @param shareId The unique identifier for the share.
     * @return The share data as a Base64-encoded string, or {@code null} if the share does not exist.
     */
    public String loadKeyShare(String userId, String shareId) {
        try {
            String collectionName = "user_" + userId;
            DocumentReference docRef = db.collection(collectionName).document("shares_" + shareId);

            // Retrieve the share data
            DocumentSnapshot document = docRef.get().get();
            if (document.exists()) {
                return document.getString("data");
            } else {
                System.out.println("Share not found: " + shareId + " for user: " + userId);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error loading key share: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a share exists in the user's Firestore collection.
     *
     * @param userId  The ID of the user whose collection will be checked.
     * @param shareId The unique identifier for the share.
     * @return {@code true} if the share exists, {@code false} otherwise.
     */
    public boolean checkShareExists(String userId, String shareId) {
        try {
            String collectionName = "user_" + userId;
            DocumentReference docRef = db.collection(collectionName).document("shares_" + shareId);

            // Check if the document exists
            boolean exists = docRef.get().get().exists();
            System.out.println("Checked share existence: " + shareId + " for user: " + userId + " - Exists: " + exists);
            return exists;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error checking share existence: " + e.getMessage());
            return false;
        }
    }

    // ------------------ Conversation Methods ------------------ //

    /**
     * Saves a conversation to a user-specific Firestore collection.
     *
     * @param conversationKey The unique key for the conversation.
     * @param conversation The Conversation object to save.
     * @param userId The ID of the user for whom the conversation is being saved.
     * @throws IOException If an error occurs during serialization.
     */
    public void saveConversation(String conversationKey, Conversation conversation, String userId) throws IOException {
        try {
            String collectionName = "user_" + userId;
            db.collection(collectionName).document(conversationKey)
                    .set(conversation).get();
            System.out.println("Conversation saved with ID: " + conversationKey + " for user: " + userId);
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Error saving conversation to Firebase for user: " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Loads a conversation from a user-specific Firestore collection.
     *
     * @param conversationKey The unique key for the conversation.
     * @param userId The ID of the user for whom the conversation is being loaded.
     * @return The Conversation object retrieved from Firestore, or null if not found.
     * @throws IOException If an error occurs during deserialization.
     */
    public Conversation loadConversation(String conversationKey, String userId) throws IOException {
        String collectionName = "user_" + userId;
        DocumentReference docRef = db.collection(collectionName).document(conversationKey);
        try {
            DocumentSnapshot document = docRef.get().get();
            if (document.exists()) {
                return document.toObject(Conversation.class);
            } else {
                System.out.println("No conversation found with ID: " + conversationKey + " for user: " + userId);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Error loading conversation from Firebase for user: " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Lists all conversation keys (document IDs) in a user-specific Firestore collection.
     *
     * @param userId The ID of the user whose conversation keys are being listed.
     * @return A list of keys representing all conversations in the user's collection.
     */
    public List<String> listAllConversationKeys(String userId) {
        List<String> keys = new ArrayList<>();
        String collectionName = "user_" + userId;
        try {
            CollectionReference conversations = db.collection(collectionName);
            QuerySnapshot snapshot = conversations.get().get();
            for (QueryDocumentSnapshot document : snapshot) {
                keys.add(document.getId());
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error listing conversations for user " + userId + ": " + e.getMessage());
        }
        return keys;
    }

    /**
     * Retrieves all conversations for a specific user from their Firestore collection.
     *
     * @param user The user whose conversations are being retrieved.
     * @return A list of Conversation objects for the specified user.
     */
    public List<Conversation> getAllConversations(User user) {
        List<Conversation> userConversations = new ArrayList<>();
        String userId = user.getUserID();
        String collectionName = "user_" + userId;

        try {
            // Retrieve all conversations in the user's collection
            List<QueryDocumentSnapshot> allDocuments = db.collection(collectionName).get().get().getDocuments();

            System.out.println("Retrieved all conversation documents from Firebase for user: " + userId);

            for (QueryDocumentSnapshot document : allDocuments) {
                Conversation conversation = document.toObject(Conversation.class);
                System.out.println("Loaded conversation with ID: " + document.getId() + " with participants: " +
                        conversation.getParticipant1().getUserID() + " and " + conversation.getParticipant2().getUserID());

                if (conversation.isParticipant(user)) {
                    System.out.println("Adding conversation for user: " + userId);
                    userConversations.add(conversation);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error retrieving conversations from Firebase for user: " + userId + ": " + e.getMessage());
        }

        return userConversations;
    }

    /**
     * Deletes a conversation from a user-specific Firestore collection.
     *
     * @param conversationKey The unique key for the conversation.
     * @param userId The ID of the user for whom the conversation is being deleted.
     */
    public void deleteConversation(String conversationKey, String userId) {
        String collectionName = "user_" + userId;
        try {
            db.collection(collectionName).document(conversationKey).delete().get();
            System.out.println("Conversation deleted with ID: " + conversationKey + " for user: " + userId);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error deleting conversation for user " + userId + ": " + e.getMessage());
        }
    }


     // ------------------ Group Methods ------------------ //

     public void saveGroup(String groupKey, Group group) throws IOException {
        try {
            db.collection("groups")
            .document(groupKey)
            .set(group)
            .get();
            System.out.println("Group saved with ID: " + groupKey);
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Error saving group to Firebase: " + e.getMessage());
        }
    }

    public Group loadGroup(String groupKey) throws IOException {
        DocumentReference docRef = db.collection("groups").document(groupKey);
        try {
            DocumentSnapshot document = docRef.get().get();
            if (document.exists()) {
                return document.toObject(Group.class);
            } else {
                System.out.println("No group found with ID: " + groupKey);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Error loading group from Firebase: " + e.getMessage());
        }
    }

    public List<String> listAllGroupIds() {
        List<String> ids = new ArrayList<>();
        try {
            CollectionReference groups = db.collection("groups");
            QuerySnapshot snapshot = groups.get().get();
            for (QueryDocumentSnapshot document : snapshot) {
                ids.add(document.getId());
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error listing groups: " + e.getMessage());
        }
        return ids;
    }
    
    /**
     * Retrieves all groups where the specified user is a member.
     *
     * @param currentUser The user whose group memberships should be checked.
     * @return A list of Group objects where the specified user is a member.
     */
    public List<Group> getAllGroupsWithMember(User currentUser) {
        List<Group> userGroups = new ArrayList<>();
        try {
            List<QueryDocumentSnapshot> allDocuments = db.collection("groups").get().get().getDocuments();
            System.out.println("Retrieved all group documents from Firebase: " + allDocuments.size());
    
            for (QueryDocumentSnapshot document : allDocuments) {
                Group group = document.toObject(Group.class);
                if (group != null && group.getMembers().contains(currentUser)) {
                    userGroups.add(group);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error retrieving groups from Firebase: " + e.getMessage());
        }
        return userGroups;
    }

    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        try {
            List<QueryDocumentSnapshot> allDocuments = db.collection("groups").get().get().getDocuments();
            System.out.println("Retrieved all group documents from Firebase: " + allDocuments.size());

            for (QueryDocumentSnapshot document : allDocuments) {
                Group group = document.toObject(Group.class);
                groups.add(group);
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error retrieving groups from Firebase: " + e.getMessage());
        }
        return groups;
    }

    public void deleteGroup(String groupId) {
        try {
            db.collection("groups").document(groupId).delete().get();
            System.out.println("Group deleted with ID: " + groupId);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error deleting group: " + e.getMessage());
        }
    }
}
