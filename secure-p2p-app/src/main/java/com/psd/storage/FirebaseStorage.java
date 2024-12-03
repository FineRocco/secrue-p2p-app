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
import java.math.BigInteger;
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
    public void saveKeyShare(String userId, String shareId, BigInteger shareData) {
        try {
            String collectionName = "user_" + userId;
            DocumentReference docRef = db.collection(collectionName).document("shares_" + shareId);

            // Save the share data
            Map<String, Object> share = new HashMap<>();
            share.put("shareId", shareId);
            share.put("data", shareData.toString());
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
    public BigInteger loadKeyShare(String userId, String shareId) {
        try {
            String collectionName = "user_" + userId;
            DocumentReference docRef = db.collection(collectionName).document("shares_" + shareId);

            // Retrieve the share data
            DocumentSnapshot document = docRef.get().get();
            if (document.exists()) {
                return new BigInteger(document.getString("data")) ;
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

    // ------------------ Dictionary Methods ------------------ //

    /**
     * Checks if the `dictionary` subcollection exists in the user's Firestore collection.
     *
     * @param userId The ID of the user whose `dictionary` subcollection will be checked.
     * @return {@code true} if the `dictionary` subcollection exists and contains at least one document, {@code false} otherwise.
     */
    public boolean checkDicExists(String userId) {
        try {
            String userCollectionName = "user_" + userId;

            // Check if the `dictionary` subcollection exists by querying a single document
            CollectionReference dictionaryRef = db.collection(userCollectionName).document("dictionaries").collection("dictionary");
            QuerySnapshot snapshot = dictionaryRef.limit(1).get().get();
            boolean exists = !snapshot.isEmpty();

            System.out.println("Checked dictionary existence for user " + userId + ": " + exists);
            return exists;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error checking dictionary existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates the `dictionary` subcollection for the user in Firestore.
     *
     * @param userId The ID of the user whose collection will be updated.
     */
    public void createDictionariesFolder(String userId) {
        try {
            String userCollectionName = "user_" + userId;

            // Add a dummy document to initialize the subcollection
            CollectionReference dictionaryRef = db.collection(userCollectionName).document("dictionaries").collection("dictionary");
            Map<String, Object> initDoc = new HashMap<>();
            initDoc.put("init", true);
            dictionaryRef.document("init").set(initDoc).get();

            System.out.println("Created dictionary subcollection in Firebase Firestore for user: " + userId);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error creating dictionary subcollection: " + e.getMessage());
        }
    }

    /**
     * Saves an encrypted word and its metadata directly into the `dictionary` subcollection in Firestore.
     *
     * @param userId           The ID of the user whose `dictionary` subcollection will be updated.
     * @param encryptedWord    The encrypted word to save (used as the document ID).
     * @param encryptedMetadata The encrypted metadata associated with the word.
     */
    public void saveWordToDic(String userId, String encryptedWord, String encryptedMetadata) {
        try {
            String userCollectionName = "user_" + userId;

            // Reference to the dictionary subcollection
            CollectionReference dictionaryRef = db.collection(userCollectionName).document("dictionaries").collection("dictionary");

            // Create a document with metadata as the value
            Map<String, Object> wordData = new HashMap<>();
            wordData.put("metadata", encryptedMetadata);

            // Save the word and metadata using the word as the document ID
            dictionaryRef.document(encryptedWord).set(wordData).get();

            System.out.println("Saved word to Firebase Firestore dictionary for user: " + userId);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error saving word to Firebase Firestore dictionary: " + e.getMessage());
        }
    }

    /**
     * Checks if a specific word (encrypted) exists in the `dictionary` subcollection for the user.
     *
     * @param userId        The ID of the user whose `dictionary` subcollection will be checked.
     * @param encryptedWord The encrypted word to check for existence.
     * @return {@code true} if the word exists, {@code false} otherwise.
     */
    public boolean checkWordExistsInDic(String userId, String encryptedWord) {
        try {
            String userCollectionName = "user_" + userId;

            // Reference to the document for the word in the dictionary subcollection
            DocumentReference wordRef = db.collection(userCollectionName)
                                        .document("dictionaries")
                                        .collection("dictionary")
                                        .document(encryptedWord);

            // Check if the document exists
            boolean exists = wordRef.get().get().exists();
            System.out.println("Checked existence of word '" + encryptedWord + "' for user " + userId + ": " + exists);
            return exists;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error checking word existence in dictionary: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads the encrypted metadata for a specified word from the `dictionary` subcollection in the user's Firestore collection.
     *
     * @param userId        The ID of the user whose `dictionary` subcollection will be accessed.
     * @param encryptedWord The encrypted word whose metadata will be loaded.
     * @return The encrypted metadata as a String if the word exists, or {@code null} if it doesn't exist.
     */
    public String loadWordMetadata(String userId, String encryptedWord) {
        try {
            String userCollectionName = "user_" + userId;

            // Reference to the dictionary subcollection
            DocumentReference wordDocRef = db.collection(userCollectionName)
                                            .document("dictionaries")
                                            .collection("dictionary")
                                            .document(encryptedWord);

            // Fetch the document
            DocumentSnapshot document = wordDocRef.get().get();

            if (document.exists()) {
                // Return the encrypted metadata
                return document.getString("metadata");
            } else {
                System.out.println("Word not found in dictionary for user " + userId + ": " + encryptedWord);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error loading word metadata from Firebase Firestore dictionary: " + e.getMessage());
            return null;
        }
    }

    // ------------------ Encrypted Conversation Methods ------------------ //

    /**
     * Saves an encrypted conversation to a user-specific Firestore collection.
     *
     * @param conversationKey The unique key for the conversation.
     * @param encryptedConversation The encrypted conversation string to save.
     * @param userId The ID of the user for whom the conversation is being saved.
     * @throws IOException If an error occurs during saving.
     */
    public void saveEncryptedConversation(String conversationKey, String encryptedConversation, String userId) throws IOException {
        try {
            String collectionName = "user_" + userId;
            Map<String, String> data = Map.of("encryptedData", encryptedConversation);

            db.collection(collectionName).document(conversationKey)
                    .set(data).get();

            System.out.println("Encrypted conversation saved with ID: " + conversationKey + " for user: " + userId);
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Error saving encrypted conversation to Firebase for user: " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Loads an encrypted conversation from a user-specific Firestore collection.
     *
     * @param conversationKey The unique key for the conversation.
     * @param userId The ID of the user for whom the conversation is being loaded.
     * @return The encrypted conversation as a string, or null if not found.
     * @throws IOException If an error occurs during loading.
     */
    public String loadEncryptedConversation(String conversationKey, String userId) throws IOException {
        String collectionName = "user_" + userId;
        DocumentReference docRef = db.collection(collectionName).document(conversationKey);

        try {
            DocumentSnapshot document = docRef.get().get();
            if (document.exists()) {
                return document.getString("encryptedData");
            } else {
                System.out.println("No encrypted conversation found with ID: " + conversationKey + " for user: " + userId);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Error loading encrypted conversation from Firebase for user: " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Lists all encrypted conversations (ID and encrypted data) in a user-specific Firestore collection.
     *
     * @param userId The ID of the user whose conversations are listed.
     * @return A map where the key is the conversation ID and the value is the encrypted data as a string.
     */
    public Map<String, String> listAllEncryptedConversations(String userId) {
        Map<String, String> encryptedConversations = new HashMap<>();
        String collectionName = "user_" + userId;

        try {
            CollectionReference conversations = db.collection(collectionName);
            QuerySnapshot snapshot = conversations.get().get();

            for (QueryDocumentSnapshot document : snapshot) {
                String conversationId = document.getId();

                // Skip conversation IDs that start with "shares_"
                if (conversationId.startsWith("shares_")) {
                    continue;
                }
                if (conversationId.startsWith("dictionaries")) {
                    continue;
                }

                String encryptedData = document.getString("encryptedData"); // Assume "encryptedData" is the field name
                if (encryptedData != null) {
                    encryptedConversations.put(conversationId, encryptedData);
                } else {
                    System.err.println("Missing encrypted data for conversation ID: " + conversationId);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error listing encrypted conversations for user " + userId + ": " + e.getMessage());
        }

        return encryptedConversations;
    }

    /**
     * Deletes an encrypted conversation from a user-specific Firestore collection.
     *
     * @param conversationKey The unique key for the conversation.
     * @param userId The ID of the user for whom the conversation is being deleted.
     */
    public void deleteEncryptedConversation(String conversationKey, String userId) {
        String collectionName = "user_" + userId;

        try {
            db.collection(collectionName).document(conversationKey).delete().get();
            System.out.println("Encrypted conversation deleted with ID: " + conversationKey + " for user: " + userId);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error deleting encrypted conversation for user " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Retrieves all encrypted conversations for a specific user from their Firestore collection.
     *
     * @param user The user whose conversations are being retrieved.
     * @return A list of encrypted conversations as strings for the specified user.
     */
    public List<String> getAllEncryptedConversations(User user) {
        List<String> encryptedConversations = new ArrayList<>();
        String userId = user.getUserID();
        String collectionName = "user_" + userId;

        try {
            // Retrieve all conversations in the user's collection
            List<QueryDocumentSnapshot> allDocuments = db.collection(collectionName).get().get().getDocuments();

            System.out.println("Retrieved all encrypted conversation documents from Firebase for user: " + userId);

            for (QueryDocumentSnapshot document : allDocuments) {
                String encryptedData = document.getString("encryptedData");
                if (encryptedData != null) {
                    encryptedConversations.add(encryptedData);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error retrieving encrypted conversations from Firebase for user: " + userId + ": " + e.getMessage());
        }

        return encryptedConversations;
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
