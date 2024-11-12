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
import com.psd.entities.User;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FirebaseStorage {
    private Firestore db = null; // Initialize to null by default
    private final String collectionName = "conversations";

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
     * Saves a conversation to Firestore.
     *
     * @param conversationKey The unique key for the conversation.
     * @param conversation The Conversation object to save.
     * @throws IOException If an error occurs during serialization.
     */
    public void saveConversation(String conversationKey, Conversation conversation) throws IOException {
        try {
            // Store the conversation object directly in Firestore
            db.collection(collectionName).document(conversationKey)
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
        DocumentReference docRef = db.collection(collectionName).document(conversationKey);
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
            CollectionReference conversations = db.collection(collectionName);
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
            List<QueryDocumentSnapshot> allDocuments = db.collection(collectionName).get().get().getDocuments();

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
            db.collection(collectionName).document(conversationKey).delete().get();
            System.out.println("Conversation deleted with ID: " + conversationKey);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error deleting conversation: " + e.getMessage());
        }
    }
}
