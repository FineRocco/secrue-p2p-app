package com.psd;

import com.psd.entities.Conversation;
import com.psd.entities.Group;
import com.psd.entities.Message;
import com.psd.entities.MessageGroup;
import com.psd.entities.User;
import com.psd.services.EncryptionService;
import com.psd.services.SSLService;
import com.psd.storage.AWS3Storage;
import com.psd.storage.AzureBlobStorage;
import com.psd.storage.FirebaseStorage;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Main class for the Secure P2P Messaging Application.
 * Initializes services and provides a terminal-based interface for interaction.
 */
public class SecureP2PMessagingApp extends Application {

    User currentUser;
    P2PServer userServer;
    private static AWS3Storage aws3Storage = AWS3Storage.getInstance();
    private static FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private static AzureBlobStorage azureBlobStorage = AzureBlobStorage.getInstance();
    public List<Group> groups = new ArrayList<>();
    public Map<String, Conversation> conversationMap = new HashMap<>();
    public List<String> conversationsIds = new ArrayList<>();

    static {
        // Register the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("P2P Messaging App");

        // Create labels and text fields
        Label usernameLabel = new Label("Username:");
        usernameLabel.setTextFill(Color.BLACK);
        TextField usernameInput = new TextField();
        usernameInput.setPromptText("Enter your username");
        TextField searchInput = new TextField();
        searchInput.setPromptText("Enter the word to search");

        Label portLabel = new Label("Port:");
        portLabel.setTextFill(Color.BLACK);
        TextField portInput = new TextField();
        portInput.setPromptText("Enter your port number");

        // Create buttons
        Button submitButton = new Button("Submit");
        Button sendDirectMessageButton = new Button("Send Direct Message");
        Button conversationsButton = new Button("Conversations");
        Button interestsButton = new Button("Interests");
        Button groupsButton = new Button("Groups");
        Button searchButton = new Button("Search");
        Button exitButton = new Button("Exit");

        // Layout
        GridPane layout = new GridPane();
        layout.setHgap(10);
        layout.setVgap(10);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 20;");

        // Add components to the layout
        layout.add(usernameLabel, 0, 0);
        layout.add(usernameInput, 1, 0);
        layout.add(portLabel, 0, 1);
        layout.add(portInput, 1, 1);
        layout.add(submitButton, 1, 2);

        // Set up the scene
        Scene scene = new Scene(layout, 350, 200);
        primaryStage.setScene(scene);
        primaryStage.show();

        //Layouts
        BorderPane mainMenuLayout = new BorderPane();
        mainMenuLayout.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dddddd; -fx-padding: 10;");

        // Set the button action to handle submission and switch to the main menu scene
        submitButton.setOnAction(e -> {
            try {
                // Extract the username and port from input fields
                String userName = usernameInput.getText();
                int userPort = Integer.parseInt(portInput.getText());
                String ipAddress = InetAddress.getLocalHost().getHostAddress();

                currentUser = new User(userName, ipAddress, userPort);
                userServer = new P2PServer(currentUser, mainMenuLayout);

                Label userDetailsLabel = new Label("User: " + userName + ", IP = " + ipAddress + ":" + userPort);
                userDetailsLabel.setTextFill(Color.DARKGRAY);

                // Create a top bar with user details
                HBox topBar = new HBox(userDetailsLabel);
                topBar.setAlignment(Pos.CENTER);
                topBar.setStyle("-fx-padding: 10; -fx-background-color: #f0f0f0;");

                // Create sidebar main menu
                VBox sideBar = new VBox(10, conversationsButton, sendDirectMessageButton, interestsButton, groupsButton, searchInput, searchButton, exitButton);
                sideBar.setAlignment(Pos.TOP_LEFT);
                sideBar.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10;");

                // Add a border to the center pane
                StackPane centerPane = new StackPane(new Label("Welcome to the Main Menu, " + userName + "!"));
                centerPane.setStyle("-fx-padding: 20;");

                mainMenuLayout.setTop(topBar);
                mainMenuLayout.setLeft(sideBar);
                mainMenuLayout.setCenter(centerPane);

                // Set up the main menu scene
                Scene mainMenuScene = new Scene(mainMenuLayout, 550, 400);
                primaryStage.setScene(mainMenuScene);

            } catch (RuntimeException ex) {
                // Handle the error if the port is not a valid integer
                portInput.setPromptText("Enter a valid port number");
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        });

        interestsButton.setOnAction(e -> {
            // Create a new Stage (popup window) for selecting interests
            Stage interestsStage = new Stage();
            interestsStage.initModality(Modality.APPLICATION_MODAL);
            interestsStage.setTitle("Select Interests");
        
            // Set up checkboxes for interests
            CheckBox footballCheckBox = new CheckBox("Football");
            CheckBox ufcCheckBox = new CheckBox("UFC");
            CheckBox basketballCheckBox = new CheckBox("Basketball");
        
            // Create a subscribe button
            Button subscribeButton = new Button("Subscribe");
            subscribeButton.setOnAction(subscribeEvent -> {
                // Clear previous interests (if needed) before adding new ones
                currentUser.setInterests(new ArrayList<>());
        
                // Add selected interests to currentUser's interests
                if (footballCheckBox.isSelected()) currentUser.addInterest("football");
                if (ufcCheckBox.isSelected()) currentUser.addInterest("ufc");
                if (basketballCheckBox.isSelected()) currentUser.addInterest("basketball");
        
                // Display a message with selected interests or handle them as needed
                System.out.println("Subscribed to: " + currentUser.getInterests());
                try {
                    // Attempt to retrieve all groups for the current user from AWS S3
                    groups = aws3Storage.getAllGroups();
                    System.out.println("Successfully retrieved groups from AWS S3.");
                } catch (Exception awsException) {
                    System.err.println("Error retrieving groups from AWS S3: " + awsException.getMessage());
                    
                    // Attempt to retrieve from Firebase if AWS S3 fails
                    System.out.println("Attempting to retrieve groups from Firebase instead...");
                    try {
                        groups = firebaseStorage.getAllGroups();
                        System.out.println("Successfully retrieved groups from Firebase.");
                    } catch (Exception firebaseException) {
                        System.err.println("Error retrieving groups from Firebase: " + firebaseException.getMessage());
                        
                        // If both AWS S3 and Firebase retrieval fail, attempt Azure Blob Storage
                        System.out.println("Attempting to retrieve groups from Azure Blob Storage instead...");
                        try {
                            groups = azureBlobStorage.getAllGroups();
                            System.out.println("Successfully retrieved groups from Azure Blob Storage.");
                        } catch (Exception azureException) {
                            System.err.println("Error retrieving groups from Azure Blob Storage: " + azureException.getMessage());
                        }
                    }
                }
                for (String interest : currentUser.getInterests()) {
                    for (Group group : groups) {
                        if (group.getGroupID().equals(interest)) {
                            group.addMember(currentUser);
                        }
                    }
                    SSLService.importUserCertificateToGroupTruststore(interest, currentUser.getUserID());
                    userServer.sendInterestsToCentralServer(currentUser);
                }
                interestsStage.close();
            });
        
            // Arrange checkboxes and subscribe button in a VBox layout
            VBox interestsLayout = new VBox(10, footballCheckBox, ufcCheckBox, basketballCheckBox, subscribeButton);
            interestsLayout.setPadding(new Insets(15));
            interestsLayout.setStyle("-fx-background-color: #f9f9f9;");
        
            // Set the scene for the popup window and show it
            Scene interestsScene = new Scene(interestsLayout, 200, 200);
            interestsStage.setScene(interestsScene);
            interestsStage.showAndWait();
        });
        

        // Set the button action to prepare a direct message
        sendDirectMessageButton.setOnAction(event -> {

            Label contactUsernameLabel = new Label("Username:");
            contactUsernameLabel.setTextFill(Color.BLACK);
            TextField contactUsernameInput = new TextField();
            contactUsernameInput.setPromptText("Enter the receiver's username");

            Label messageLabel = new Label("Message:");
            messageLabel.setTextFill(Color.BLACK);
            TextArea messageInput = new TextArea();
            messageInput.setWrapText(true);
            messageInput.setPrefHeight(100);
            messageInput.setPromptText("Enter your message");

            Button sendButton = new Button("Send");

            // Layout
            VBox sendDirectMessageLayout = new VBox(10);
            sendDirectMessageLayout.setAlignment(Pos.CENTER);
            sendDirectMessageLayout.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10;");
            sendDirectMessageLayout.getChildren().addAll(
                    contactUsernameLabel, contactUsernameInput,
                    messageLabel, messageInput, sendButton
            );

            mainMenuLayout.setCenter(sendDirectMessageLayout);


            // Set the button action to send a direct message and create a new conversation
            sendButton.setOnAction(submitEvent -> {
                String receiverName = contactUsernameInput.getText();
                String messageContent = messageInput.getText();

                try {
                    if (!receiverName.isEmpty() && !messageContent.isEmpty()) {
                        User receiver = new User(receiverName, null, 0);

                        // Create a new message
                        Message message = Message.createMessage(currentUser, receiver, messageContent);
                        boolean success = userServer.sendDirectMessage(message, currentUser, receiver);

                        Label confirmationLabel = new Label(success ? "Message sent to " + receiverName :
                                "Failed to send message to " + receiverName);
                        confirmationLabel.setTextFill(success ? Color.GREEN : Color.RED);
                        mainMenuLayout.setCenter(new StackPane(confirmationLabel));

                    } else {
                        if (receiverName.isEmpty()) {
                            contactUsernameInput.setPromptText("Enter username");
                        }
                        if (messageContent.isEmpty()) {
                            messageInput.setPromptText("Enter a message");
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });

        // Handle conversationsButton click
        conversationsButton.setOnAction(event -> {
            Map<String, String> encrpytedConversations = new HashMap<>();
            try {
                // Attempt to retrieve all conversations for the current user from AWS S3
                encrpytedConversations = aws3Storage.listAllEncryptedConversations(currentUser);
                System.out.println("Successfully retrieved conversations from AWS S3.");
            } catch (Exception e) {
                System.err.println("Error retrieving conversations from AWS S3: " + e.getMessage());
                
                // Attempt to retrieve from Firebase if AWS S3 fails
                System.out.println("Attempting to retrieve conversations from Firebase instead...");
                try {
                    encrpytedConversations = firebaseStorage.listAllEncryptedConversations(currentUser.getUserID());
                    System.out.println("Successfully retrieved conversations from Firebase.");
                } catch (Exception firebaseException) {
                    System.err.println("Error retrieving conversations from Firebase: " + firebaseException.getMessage());
                    
                    // If both AWS S3 and Firebase retrieval fail, attempt Azure Blob Storage
                    System.out.println("Attempting to retrieve conversations from Azure Blob Storage instead...");
                    try {
                        encrpytedConversations = azureBlobStorage.listAllEncryptedConversations(currentUser.getUserID());
                        System.out.println("Successfully retrieved conversations from Azure Blob Storage.");
                    } catch (Exception azureException) {
                        System.err.println("Error retrieving conversations from Azure Blob Storage: " + azureException.getMessage());
                    }
                }
            }

            // Decrypt the conversations and add them to the list
            for (Map.Entry<String, String> entry : encrpytedConversations.entrySet()) {
                String conversationId = entry.getKey();
                String encryptedConversation = entry.getValue();
                try {
                if(!conversationsIds.contains(conversationId)) {
                    // Decrypt the conversation and add it to the list
                    Conversation conversation = (Conversation) EncryptionService.decryptObject(P2PServer.secretKeyCloud, encryptedConversation);

                    conversationsIds.add(conversationId);
                    conversationMap.put(conversationId, conversation);

                    System.out.println("Successfully decrypted conversation with ID: " + conversationId);
                    }

                } catch (ClassNotFoundException | GeneralSecurityException | IOException e) {
                    System.err.println("Error decrypting conversation with ID: " + conversationId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Create a layout to display the conversations
            VBox conversationsLayout = new VBox(10);
            conversationsLayout.setAlignment(Pos.CENTER);
            conversationsLayout.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 30; -fx-border-color: #ccc;" +
                    " -fx-border-width: 1;");

            if (conversationsIds.isEmpty()) {
                // Display message if no conversations found
                Label noConversationsLabel = new Label("No conversations found.");
                conversationsLayout.getChildren().add(noConversationsLabel);
            } else {
                // Label for selecting a conversation
                Label selectConversationLabel = new Label("Select a conversation:");
                selectConversationLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

                // List of conversations
                ComboBox<String> conversationComboBox = new ComboBox<>();
                conversationComboBox.setPrefWidth(300);


                for (int i = 0; i < conversationsIds.size(); i++) {
                    Conversation conversation = conversationMap.get(conversationsIds.get(i));
                    User participant = conversation.getParticipant1().equals(currentUser) ? conversation.getParticipant2() : conversation.getParticipant1();

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                    Instant instant = Instant.parse(conversation.getStartTime());
                    // Convert the Instant to ZonedDateTime using the system's default time zone
                    String formattedTimestamp = instant
                                                    .atZone(ZoneId.systemDefault())
                                                    .format(formatter);
                    
                    String conversationLabel = (i + 1) + ". Conversation with " + participant.getUserID() +
                                              " (Started on " + formattedTimestamp + ")";                    

                    conversationComboBox.getItems().add(conversationLabel);
                    conversationMap.put(conversationLabel, conversation);
                }

                Button viewConversationButton = new Button("View Conversation");
                viewConversationButton.setStyle("-fx-font-size: 12; -fx-background-color: #4CAF50; -fx-text-fill: white;" +
                        " -fx-padding: 5 10 5 10;");

                // Add elements to layout
                conversationsLayout.getChildren().addAll(selectConversationLabel, conversationComboBox, viewConversationButton);

                // Handle view conversation button click
                viewConversationButton.setOnAction(viewEvent -> {
                    String selectedConvoLabel = conversationComboBox.getValue();
                    if (selectedConvoLabel != null) {
                        Conversation selectedConvo = conversationMap.get(selectedConvoLabel);
                        User otherParticipant = selectedConvo.getParticipant1().equals(currentUser) ?
                                selectedConvo.getParticipant2() : selectedConvo.getParticipant1();

                        // Create a vertical layout to hold the conversation details and input area
                        VBox conversationLayout = new VBox(10);
                        conversationLayout.setAlignment(Pos.CENTER);
                        conversationLayout.setStyle("-fx-background-color: #ffffff; -fx-padding: 20; -fx-border-color: #ccc; -fx-border-width: 1;");

                        Label conversationHeader = new Label("--- Conversation with " + otherParticipant.getUserID() + " ---");
                        conversationHeader.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10;");

                        // Chat logs area
                        VBox chatLogs = new VBox(10);
                        chatLogs.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 10;");

                        for (Message msg : selectedConvo.getMessages()) { // Use getMessages() directly
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                            
                            // Parse the timestamp string back into an Instant
                            Instant instant = Instant.parse(msg.getTimestamp());
                            
                            // Convert the Instant to ZonedDateTime using the system's default time zone
                            String formattedTimestamp = instant
                                                            .atZone(ZoneId.systemDefault())
                                                            .format(formatter);
                            
                            String senderName = msg.getSender().equals(currentUser) ? "You" : otherParticipant.getUserID();
                            Label messageLabel = new Label(senderName + " [" + formattedTimestamp + "]: " + msg.getContent());
                            chatLogs.getChildren().add(messageLabel);
                        }
                        
                        // Wrap the chatLogs VBox in a ScrollPane
                        ScrollPane scrollPane = new ScrollPane(chatLogs);
                        scrollPane.setFitToWidth(true);
                        scrollPane.setPrefHeight(400); // Adjust the height as needed to fit the layout

                        // New message input box
                        HBox inputBox = new HBox(10);
                        inputBox.setAlignment(Pos.CENTER_LEFT);

                        TextArea newMessageInput = new TextArea();
                        newMessageInput.setWrapText(true);
                        newMessageInput.setPrefHeight(50);

                        // Send message button
                        Button sendMessageButton = new Button("Send Message");
                        sendMessageButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 5 10 5 10;");

                        inputBox.getChildren().addAll(newMessageInput, sendMessageButton);

                        // Add the top chat logs and bottom input box to the vertical layout
                        conversationLayout.getChildren().addAll(conversationHeader, scrollPane, inputBox);

                        // Set action for send message button
                        sendMessageButton.setOnAction(sendEvent -> {
                            String messageContent = newMessageInput.getText();

                            if (!messageContent.isEmpty()) {
                                // Create and send the message
                                Message newMessage = Message.createMessage(currentUser, otherParticipant, messageContent);
                                System.out.println("Sending message: " + newMessage + " to " + otherParticipant.getUserID() + " from " + currentUser.getUserID());
                                boolean success = userServer.sendDirectMessage(newMessage, currentUser, otherParticipant);

                                newMessageInput.clear();

                                // Dynamically add the new message to the chat log
                                String senderName = "You";
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                                // Parse the timestamp string back into an Instant
                                Instant instant = Instant.parse(newMessage.getTimestamp());

                                // Convert the Instant to ZonedDateTime using the system's default time zone
                                String formattedTimestamp = instant
                                                            .atZone(ZoneId.systemDefault())
                                                            .format(formatter);
                                
                                Label messageLabel = new Label(senderName + " [" + formattedTimestamp + "]: " + newMessage.getContent());
                                chatLogs.getChildren().add(messageLabel); // Update the chat log with the new message
                                

                                // Optionally, scroll to the bottom of the chat log
                                scrollPane.setVvalue(1.0); // This ensures the scroll pane moves to the latest message
                                if (!success) {
                                    Label statusLabel = new Label("Failed to send message to " + otherParticipant.getUserID());
                                    conversationLayout.getChildren().add(statusLabel);
                                }
                            }
                        });

                        // Set the conversation layout as the center pane of the main layout
                        mainMenuLayout.setCenter(conversationLayout);
                    } else {
                        conversationComboBox.setPromptText("Select a conversation");
                    }
                });
            }

            // Set the conversations layout to the center of the main layout
            mainMenuLayout.setCenter(conversationsLayout);
        });

        // Handle search button click
        searchButton.setOnAction(event -> {
            String searchWord = searchInput.getText();

            if (searchWord == null || searchWord.isEmpty()) {
                searchInput.setPromptText("Enter a word to search");
                return;
            }

            // Clear the previous results from the center pane
            mainMenuLayout.setCenter(null);

            // Vertical layout to display search results
            VBox searchResultsLayout = new VBox(10);
            searchResultsLayout.setAlignment(Pos.CENTER);
            searchResultsLayout.setStyle("-fx-background-color: #ffffff; -fx-padding: 20; -fx-border-color: #ccc; -fx-border-width: 1;");

            Label searchHeader = new Label("--- Search Results for: " + searchWord + " ---");
            searchHeader.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 10;");
            searchResultsLayout.getChildren().add(searchHeader);

            VBox resultsBox = new VBox(10);
            resultsBox.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 10;");

            // Fetch search results from cloud
            Map<String, List<String>> searchResults = userServer.searchMessagesByWord(currentUser, searchWord);

            if (searchResults.isEmpty()) {
                Label noResultsLabel = new Label("No messages found for the word: " + searchWord);
                noResultsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");
                searchResultsLayout.getChildren().add(noResultsLabel);
            } else {
                // Extract and display search results
                List<String> messageContents = searchResults.getOrDefault("messageContent", new ArrayList<>());
                List<String> conversationIds = searchResults.getOrDefault("conversationId", new ArrayList<>());
                List<String> timestamps = searchResults.getOrDefault("timestamp", new ArrayList<>());

                for (int i = 0; i < messageContents.size(); i++) {
                    // Safely retrieve conversationId and timestamp (if available)
                    String messageContent = messageContents.get(i);
                    String conversationId = i < conversationIds.size() ? conversationIds.get(i) : "Unknown Conversation ID";
                    String timestamp = i < timestamps.size() ? timestamps.get(i) : "Unknown Timestamp";

                    // Format the timestamp
                    String formattedTimestamp;
                    try {
                        Instant instant = Instant.parse(timestamp);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        formattedTimestamp = instant.atZone(ZoneId.systemDefault()).format(formatter);
                    } catch (Exception e) {
                        formattedTimestamp = "Invalid Timestamp";
                    }

                    // Create a label for each result
                    Label resultLabel = new Label(
                            "Conversation ID: " + conversationId + "\n" +
                            "Timestamp: " + formattedTimestamp + "\n" +
                            "Message: " + messageContent
                    );

                    resultLabel.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ccc; -fx-border-width: 1; -fx-padding: 10; -fx-margin: 5;");
                    resultsBox.getChildren().add(resultLabel);
                }
            }

            // Wrap results in a ScrollPane
            ScrollPane scrollPane = new ScrollPane(resultsBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(400); // Adjust height as necessary

            // Add results to the search layout
            searchResultsLayout.getChildren().add(scrollPane);

            // Set the search layout to the center of the main layout
            mainMenuLayout.setCenter(searchResultsLayout);
        });

        exitButton.setOnAction(event -> {
            System.out.println("Exiting...");
            primaryStage.close(); // Close the primary stage to exit the application
            System.exit(0); // Ensures the program is terminated properly
        });

        // Handle groupsButton click
        groupsButton.setOnAction(event -> {
            try {
                // Attempt to retrieve all groups for the current user from AWS S3
                groups = aws3Storage.getAllGroupsWithMember(currentUser);
                System.out.println("Successfully retrieved groups from AWS S3.");
            } catch (Exception e) {
                System.err.println("Error retrieving groups from AWS S3: " + e.getMessage());
                
                // Attempt to retrieve from Firebase if AWS S3 fails
                System.out.println("Attempting to retrieve groups from Firebase instead...");
                try {
                    groups = firebaseStorage.getAllGroupsWithMember(currentUser);
                    System.out.println("Successfully retrieved groups from Firebase.");
                } catch (Exception firebaseException) {
                    System.err.println("Error retrieving groups from Firebase: " + firebaseException.getMessage());
                    
                    // If both AWS S3 and Firebase retrieval fail, attempt Azure Blob Storage
                    System.out.println("Attempting to retrieve groups from Azure Blob Storage instead...");
                    try {
                        groups = azureBlobStorage.getAllGroupsWithMember(currentUser);
                        System.out.println("Successfully retrieved groups from Azure Blob Storage.");
                    } catch (Exception azureException) {
                        System.err.println("Error retrieving groups from Azure Blob Storage: " + azureException.getMessage());
                    }
                }
            }

            System.out.println("Found " + groups.size() + " authorized groups for " + currentUser.getUserID());

            // Create a layout to display the groups
            VBox groupsLayout = new VBox(10);
            groupsLayout.setAlignment(Pos.CENTER);
            groupsLayout.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 30; -fx-border-color: #ccc; -fx-border-width: 1;");

            if (groups.isEmpty()) {
                // Display message if no groups found
                Label noGroupsLabel = new Label("No groups found.");
                groupsLayout.getChildren().add(noGroupsLabel);
            } else {
                // Label for selecting a group
                Label selectGroupLabel = new Label("Select a group:");
                selectGroupLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

                // List of groups
                ComboBox<String> groupComboBox = new ComboBox<>();
                groupComboBox.setPrefWidth(300);

                Map<String, Group> groupMap = new HashMap<>();
                for (int i = 0; i < groups.size(); i++) {
                    Group group = groups.get(i);
                    String groupLabel = (i + 1) + ". " + group.getGroupID();
                    groupComboBox.getItems().add(groupLabel);
                    groupMap.put(groupLabel, group);
                }

                Button viewGroupButton = new Button("View Group");
                viewGroupButton.setStyle("-fx-font-size: 12; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 5 10 5 10;");

                // Add elements to layout
                groupsLayout.getChildren().addAll(selectGroupLabel, groupComboBox, viewGroupButton);

                List<Group> finalGroups = groups; // Make groups effectively final
                // Handle view group button click
                viewGroupButton.setOnAction(viewEvent -> {
                    System.out.print("Final groups: ");
                    String selectedGroupLabel = groupComboBox.getValue();
                    for (Group group : finalGroups) {
                        System.out.println(group.getGroupID() + " ");
                    }
                    System.out.println("Selected group label: " + selectedGroupLabel);
                    if (selectedGroupLabel != null) {
                        // Find the selected group in the list of groups by matching the ID
                        Group selectedGroup = finalGroups.stream()
                            .filter(group -> selectedGroupLabel.contains(group.getGroupID()))
                            .findFirst()
                            .orElse(null);
                        
                        System.out.println("Selected group: " + selectedGroup.getGroupID());

                        // Create a vertical layout to hold the group details
                        VBox groupLayout = new VBox(10);
                        groupLayout.setAlignment(Pos.CENTER);
                        groupLayout.setStyle("-fx-background-color: #ffffff; -fx-padding: 20; -fx-border-color: #ccc; -fx-border-width: 1;");

                        Label groupHeader = new Label("--- Group: " + selectedGroup.getGroupID() + " ---");
                        groupHeader.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-padding: 10;");

                        // Group messages area
                        VBox groupMessages = new VBox(10);
                        groupMessages.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 10;");

                        System.out.println("Group messages: " + selectedGroup.getMessages().values());

                        for (MessageGroup msgGroup : selectedGroup.getMessages().values()) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                            String formattedTimestamp = msgGroup.getTimestamp().atZone(ZoneId.systemDefault()).format(formatter);
                            String senderName = msgGroup.getSender().getUserID();

                            // Display decrypted content in the label
                            Label messageLabel = new Label(senderName + " [" + formattedTimestamp + "]: " + (msgGroup.getContent()));
                            groupMessages.getChildren().add(messageLabel);
                        }

                        // Wrap the groupMessages VBox in a ScrollPane
                        ScrollPane scrollPane = new ScrollPane(groupMessages);
                        scrollPane.setFitToWidth(true);
                        scrollPane.setPrefHeight(400); // Adjust the height as needed

                        // New message input box
                        HBox inputBox = new HBox(10);
                        inputBox.setAlignment(Pos.CENTER_LEFT);

                        TextArea newMessageInput = new TextArea();
                        newMessageInput.setWrapText(true);
                        newMessageInput.setPrefHeight(50);

                        // Send message button
                        Button sendMessageButton = new Button("Send Message");
                        sendMessageButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 5 10 5 10;");

                        inputBox.getChildren().addAll(newMessageInput, sendMessageButton);

                        // Add the top group messages and bottom input box to the vertical layout
                        groupLayout.getChildren().addAll(groupHeader, scrollPane, inputBox);

                        // Set action for send message button
                        sendMessageButton.setOnAction(sendEvent -> {
                            String messageContent = newMessageInput.getText();

                            if (!messageContent.isEmpty()) {
                                // Create and send the message
                                MessageGroup newMessage = new MessageGroup(currentUser, selectedGroup.getGroupID(), messageContent);
                                System.out.println("Sending message: " + newMessage + " to group " + selectedGroup.getGroupID());
                                boolean success = userServer.sendMessageGroup(newMessage, currentUser, selectedGroup);

                                newMessageInput.clear();

                                // Dynamically add the new message to the group messages
                                String senderName = "You";
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                                String formattedTimestamp = newMessage.getTimestamp().atZone(ZoneId.systemDefault()).format(formatter);
                                Label messageLabel = new Label(senderName + " [" + formattedTimestamp + "]: " + newMessage.getContent());
                                groupMessages.getChildren().add(messageLabel); // Update the chat log with the new message

                                // Optionally, scroll to the bottom of the group messages
                                scrollPane.setVvalue(1.0);
                                if (!success) {
                                    Label statusLabel = new Label("Failed to send message to group " + selectedGroup.getGroupID());
                                    groupLayout.getChildren().add(statusLabel);
                                }
                            }
                        });

                        // Set the group layout as the center pane of the main layout
                        mainMenuLayout.setCenter(groupLayout);
                    } else {
                        groupComboBox.setPromptText("Select a group");
                    }
                });
            }

            // Set the groups layout to the center of the main layout
            mainMenuLayout.setCenter(groupsLayout);
        });

    }


}
