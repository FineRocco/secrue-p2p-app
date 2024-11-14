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
import java.util.List;
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

    // ------------------ Conversation Methods ------------------ //

    /**
     * Saves a conversation to Firestore.
     *
     * @param conversationKey The unique key for the conversation.
     * @param conversation The Conversation object to save.
     * @throws IOException If an error occurs during serialization.
     */
    public void saveConversation(String conversationKey, Conversation conversation) throws IOException {
        try {
            // Store the conversation object directly in Firestore
            db.collection("conversations").document(conversationKey)
                    .set(conversation).get();
            System.out.println("Conversation saved with ID: " + conversationKey);
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Error saving conversation to Firebase: " + e.getMessage());
        }
    }

    /**
     * Loads a conversation from Firestore.
     *
     * @param conversationKey The unique key for the conversation.
     * @return The Conversation object retrieved from Firestore, or null if not found.
     * @throws IOException If an error occurs during deserialization.
     */
    public Conversation loadConversation(String conversationKey) throws IOException {
        DocumentReference docRef = db.collection("conversations").document(conversationKey);
        try {
            DocumentSnapshot document = docRef.get().get();
            if (document.exists()) {
                return document.toObject(Conversation.class);
            } else {
                System.out.println("No conversation found with ID: " + conversationKey);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Error loading conversation from Firebase: " + e.getMessage());
        }
    }

    /**
     * Lists all conversation keys (document IDs) in the Firestore collection.
     *
     * @return A list of keys representing all conversations in the collection.
     */
    public List<String> listAllConversationKeys() {
        List<String> keys = new ArrayList<>();
        try {
            CollectionReference conversations = db.collection("conversations");
            QuerySnapshot snapshot = conversations.get().get();
            for (QueryDocumentSnapshot document : snapshot) {
                keys.add(document.getId());
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error listing conversations: " + e.getMessage());
        }
        return keys;
    }

    public List<Conversation> getAllConversations(User user) {
        List<Conversation> userConversations = new ArrayList<>();

        try {
            // Retrieve all conversations in the "conversations" collection
            List<QueryDocumentSnapshot> allDocuments = db.collection("conversations").get().get().getDocuments();

            System.out.println("Retrieved all conversation documents from Firebase: " + allDocuments.size()); // Log count

            for (QueryDocumentSnapshot document : allDocuments) {
                Conversation conversation = document.toObject(Conversation.class);
                System.out.println("Loaded conversation with ID: " + document.getId() + " with participants: " +
                        conversation.getParticipant1().getUserID() + " and " + conversation.getParticipant2().getUserID());

                if (conversation.isParticipant(user)) {
                    System.out.println("Adding conversation for user: " + user.getUserID());
                    userConversations.add(conversation);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error retrieving conversations from Firebase: " + e.getMessage());
        }

        return userConversations;
    }

    /**
     * Deletes a conversation from Firestore.
     *
     * @param conversationKey The unique key for the conversation.
     */
    public void deleteConversation(String conversationKey) {
        try {
            db.collection("conversations").document(conversationKey).delete().get();
            System.out.println("Conversation deleted with ID: " + conversationKey);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error deleting conversation: " + e.getMessage());
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
