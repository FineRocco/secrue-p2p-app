package com.psd;

import com.psd.entities.Conversation;
import com.psd.entities.Message;
import com.psd.entities.User;
import com.psd.storage.AWS3Storage;
import com.psd.storage.AzureBlobStorage;
import com.psd.storage.FirebaseStorage;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Main class for the Secure P2P Messaging Application.
 * Initializes services and provides a terminal-based interface for interaction.
 */
public class SecureP2PMessagingApp extends Application {

    User currentUser;
    P2PServer userServer;
    AWS3Storage aws3Storage = new AWS3Storage();
    FirebaseStorage firebaseStorage = new FirebaseStorage();
    AzureBlobStorage azureBlobStorage = new AzureBlobStorage();

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

        Label portLabel = new Label("Port:");
        portLabel.setTextFill(Color.BLACK);
        TextField portInput = new TextField();
        portInput.setPromptText("Enter your port number");

        // Create buttons
        Button submitButton = new Button("Submit");
        Button sendDirectMessageButton = new Button("Send Direct Message");
        Button conversationsButton = new Button("Conversations");
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
                VBox sideBar = new VBox(10, conversationsButton, sendDirectMessageButton, exitButton);
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
                        Message message = new Message("1", currentUser, receiver, messageContent);
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
            List<Conversation> conversations = new ArrayList<>();
            try {
                // Attempt to retrieve all conversations for the current user from AWS S3
                conversations = aws3Storage.getAllConversations(currentUser);
                System.out.println("Successfully retrieved conversations from AWS S3.");
            } catch (Exception e) {
                System.err.println("Error retrieving conversations from AWS S3: " + e.getMessage());
                
                // Attempt to retrieve from Firebase if AWS S3 fails
                System.out.println("Attempting to retrieve conversations from Firebase instead...");
                try {
                    conversations = firebaseStorage.getAllConversations(currentUser);
                    System.out.println("Successfully retrieved conversations from Firebase.");
                } catch (Exception firebaseException) {
                    System.err.println("Error retrieving conversations from Firebase: " + firebaseException.getMessage());
                    
                    // If both AWS S3 and Firebase retrieval fail, attempt Azure Blob Storage
                    System.out.println("Attempting to retrieve conversations from Azure Blob Storage instead...");
                    try {
                        conversations = azureBlobStorage.getAllConversations(currentUser);
                        System.out.println("Successfully retrieved conversations from Azure Blob Storage.");
                    } catch (Exception azureException) {
                        System.err.println("Error retrieving conversations from Azure Blob Storage: " + azureException.getMessage());
                    }
                }
            }
            //DEBUGGING
            System.out.println("Found " + conversations.size() + " conversations for " + currentUser.getUserID());
            System.out.println("----------------------------------------------------");
            for (Conversation conv : conversations) {
                System.out.println("Conversation: " + conv.getConversationId() + " between " +
                        conv.getParticipant1().getUserID() + " and " + conv.getParticipant2().getUserID());
                for (Message msg : conv.getMessages()) {
                    System.out.println("Message: " + msg.getContent());
                }
                System.out.println("----------------------------------------------------");
            }
            //DEBUGGING

            // Create a layout to display the conversations
            VBox conversationsLayout = new VBox(10);
            conversationsLayout.setAlignment(Pos.CENTER);
            conversationsLayout.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 30; -fx-border-color: #ccc;" +
                    " -fx-border-width: 1;");

            if (conversations.isEmpty()) {
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

                Map<String, Conversation> conversationMap = new HashMap<>();
                for (int i = 0; i < conversations.size(); i++) {
                    Conversation conversation = conversations.get(i);
                    User participant = conversation.getParticipant1().equals(currentUser) ? conversation.getParticipant2() : conversation.getParticipant1();

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                    // Convert the Instant to ZonedDateTime using the system's default time zone
                    String formattedTimestamp = conversation.getStartTime()
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

                        for (Message msg : selectedConvo.getMessages()) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                        
                            // Convert the Instant to ZonedDateTime using the system's default time zone
                            String formattedTimestamp = msg.getTimestamp()
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
                                Message newMessage = new Message("1", currentUser, otherParticipant, messageContent);
                                System.out.println("Sending message: " + newMessage + " to " + otherParticipant.getUserID() + " from " + currentUser.getUserID());
                                boolean success = userServer.sendDirectMessage(newMessage, currentUser, otherParticipant);

                                newMessageInput.clear();

                                // Dynamically add the new message to the chat log
                                String senderName = "You";
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                                // Convert the Instant to ZonedDateTime using the system's default time zone
                                String formattedTimestamp = newMessage.getTimestamp()
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

        exitButton.setOnAction(event -> {
            System.out.println("Exiting...");
            primaryStage.close(); // Close the primary stage to exit the application
            System.exit(0); // Ensures the program is terminated properly
        });

    }
}
