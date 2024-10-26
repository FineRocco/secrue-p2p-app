package com.psd;

import com.psd.entities.*;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.format.DateTimeFormatter;

/**
 * Main class for the Secure P2P Messaging Application.
 * Initializes services and provides a terminal-based interface for interaction.
 */
public class SecureP2PMessagingApp extends Application{

    static BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

    static PublicKey publicKey = null;
    static PrivateKey privateKey = null;

    User currentUser;

    //P2PNetwork userNetwork;
    P2PServer userServer;

    public static void main(String[] args) throws IOException {

        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            try (InputStream keyStoreStream = new FileInputStream("keystore.jks")) {
                ks.load(keyStoreStream, "psd2024".toCharArray());
            }
        
            // Load the private key from the keystore
            Key key = ks.getKey("selfsigned", "psd2024".toCharArray()); 
            if (key instanceof PrivateKey) {
                privateKey = (PrivateKey) key;
        
                // Load the corresponding public key from the certificate
                java.security.cert.Certificate cert = ks.getCertificate("selfsigned");
                publicKey = cert.getPublicKey();
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | IOException e) {
            e.printStackTrace();
        }

        launch(args);

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("User Details");

        // Create labels and text fields
        Label usernameLabel = new Label("Username:");
        TextField usernameInput = new TextField();

        Label portLabel = new Label("Port:");
        TextField portInput = new TextField();

        // Create buttons
        Button submitButton = new Button("Submit");
        Button sendDirectMessageButton = new Button("Send Direct Message");
        Button conversationsButton = new Button("Conversations");
        Button exitButton = new Button("Exit");

        // Layout using GridPane for alignment
        GridPane layout = new GridPane();
        layout.setHgap(10);
        layout.setVgap(10);
        layout.setAlignment(Pos.CENTER); // Center the GridPane itself

        // Add components to the layout
        layout.add(usernameLabel, 0, 0);
        layout.add(usernameInput, 1, 0);
        layout.add(portLabel, 0, 1);
        layout.add(portInput, 1, 1);
        layout.add(submitButton, 1, 2);

        // Set up the scene
        Scene scene = new Scene(layout, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.show();

        //Layouts
        BorderPane mainMenuLayout = new BorderPane();

        // Set the button action to handle submission and switch to the main menu scene
        submitButton.setOnAction(e -> {
            try {
                // Extract the username and port from input fields
                String userName = usernameInput.getText();
                int userPort = Integer.parseInt(portInput.getText());

                // Initialize the P2P network for the user
                // Assuming P2PNetwork and User classes exist with the required constructors
                /*userNetwork = new P2PNetwork(userPort);

                userNetwork.connectPeer(currentUser, mainMenuLayout);
                */
                currentUser = new User(userName, publicKey, privateKey, "127.0.0.1", userPort);
                userServer = new P2PServer(userPort, mainMenuLayout);


                // Create a top bar with user details
                HBox topBar = new HBox();
                topBar.setSpacing(10);
                topBar.setAlignment(Pos.CENTER);
                topBar.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-padding: 10;");
                Label userDetailsLabel = new Label("User: " + userName + ", IP= 127.0.0.1: " + userPort);
                topBar.getChildren().add(userDetailsLabel);

                // Create side bar main menu
                VBox sideBar = new VBox();
                sideBar.setSpacing(10);
                sideBar.setAlignment(Pos.CENTER_LEFT);
                sideBar.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-padding: 10;");
                sideBar.getChildren().addAll(conversationsButton, sendDirectMessageButton, exitButton);

                // Create the main menu layout with a BorderPane
                mainMenuLayout.setTop(topBar);
                mainMenuLayout.setLeft(sideBar);

                // Add a border to the center pane
                StackPane centerPane = new StackPane();
                centerPane.setStyle("-fx-border-color: black; -fx-border-width: 2; -fx-padding: 10;");
                Label welcomeLabel = new Label("Welcome to the Main Menu, " + userName + "!");
                centerPane.getChildren().add(welcomeLabel);
                mainMenuLayout.setCenter(centerPane);

                // Add a border to the BorderPane itself
                mainMenuLayout.setStyle("-fx-border-color: black; -fx-border-width: 2;");

                // Set up the main menu scene
                Scene mainMenuScene = new Scene(mainMenuLayout, 600, 400);
                primaryStage.setScene(mainMenuScene);

            } catch (NumberFormatException ex) {
                // Handle the error if the port is not a valid integer
                portInput.setText("Enter a valid port number");
            }
        });

        sendDirectMessageButton.setOnAction(event -> {
            // Create the input fields and labels for adding a contact
            Label contactUsernameLabel = new Label("Enter username:");
            TextField contactUsernameInput = new TextField();
        
            Label contactIpLabel = new Label("Enter the user's IP address:");
            TextField contactIpInput = new TextField();
        
            Label contactPortLabel = new Label("Enter the user's port:");
            TextField contactPortInput = new TextField();

            // Input fields for message
            Label messageLabel = new Label("Enter your message:");
            TextArea messageInput = new TextArea();
            messageInput.setWrapText(true);
        
            Button sendButton = new Button("Send");
        
            // Create a new layout for the center pane to add contact details
            VBox sendDirectMessageLayout = new VBox(10);
            sendDirectMessageLayout.setAlignment(Pos.CENTER);
            sendDirectMessageLayout.getChildren().addAll(contactUsernameLabel, contactUsernameInput, contactIpLabel, contactIpInput, contactPortLabel, contactPortInput,messageLabel, messageInput, sendButton);
        
            // Set the new center pane to the BorderPane
            mainMenuLayout.setCenter(sendDirectMessageLayout);
        
            // Set the action for the add contact submit button
            sendButton.setOnAction(submitEvent -> {
                String receiverName = contactUsernameInput.getText();
                String messageContent = messageInput.getText();
                User receiver = new User(receiverName, publicKey, privateKey, contactIpInput.getText(), Integer.parseInt(contactPortInput.getText()));
                if (receiverName != null && !messageContent.isEmpty()) {
                    // Create and send the message
                    Message message = new Message("1", currentUser, receiver, messageContent);
                    boolean success = userServer.sendDirectMessage(message, currentUser, receiver);

                    // Confirmation message
                    Label confirmationLabel;
                    if (success) {
                        confirmationLabel = new Label("Message sent to " + receiverName);
                    } else {
                        confirmationLabel = new Label("Failed to send message to " + receiverName);
                    }

                    // Update the center pane with the confirmation message
                    mainMenuLayout.setCenter(new StackPane(confirmationLabel));
 
                } else {
                    // Handle case where no contact or message is provided
                    if (receiverName == null) {
                        contactUsernameInput.setPromptText("Enter username");
                    }
                    if (messageContent.isEmpty()) {
                        messageInput.setPromptText("Enter a message");
                    }
                }
            });
        });

        // Handle conversationsButton click
        conversationsButton.setOnAction(event -> {
            // Retrieve all conversations for the current user
            List<Conversation> conversations = userServer.getAllConversations(currentUser);

            // Create a layout to display the conversations
            VBox conversationsLayout = new VBox(10);
            conversationsLayout.setAlignment(Pos.CENTER);

            if (conversations.isEmpty()) {
                // Display message if no conversations found
                Label noConversationsLabel = new Label("No conversations found.");
                conversationsLayout.getChildren().add(noConversationsLabel);
            } else {
                // Label for selecting a conversation
                Label selectConversationLabel = new Label("Select a conversation:");
                conversationsLayout.getChildren().add(selectConversationLabel);

                // List of conversations
                ComboBox<String> conversationComboBox = new ComboBox<>();
                Map<String, Conversation> conversationMap = new HashMap<>();

                for (int i = 0; i < conversations.size(); i++) {
                    Conversation convo = conversations.get(i);
                    User participant = convo.getParticipant1().equals(currentUser) ? convo.getParticipant2() : convo.getParticipant1();
                    String convoLabel = (i + 1) + ". Conversation with " + participant.getUserName() + " (Started on " + convo.getStartTime() + ")";
                    conversationComboBox.getItems().add(convoLabel);
                    conversationMap.put(convoLabel, convo);
                }

                Button viewConversationButton = new Button("View Conversation");

                // Add elements to layout
                conversationsLayout.getChildren().addAll(conversationComboBox, viewConversationButton);

                // Handle view conversation button click
                viewConversationButton.setOnAction(viewEvent -> {
                    String selectedConvoLabel = conversationComboBox.getValue();
                    if (selectedConvoLabel != null) {
                        Conversation selectedConvo = conversationMap.get(selectedConvoLabel);
                        User otherParticipant = selectedConvo.getParticipant1().equals(currentUser) ? selectedConvo.getParticipant2() : selectedConvo.getParticipant1();

                        // Create a vertical layout to hold the conversation details and input area
                        VBox conversationLayout = new VBox(10);
                        conversationLayout.setAlignment(Pos.CENTER);

                        // Top horizontal box for chat logs
                        HBox chatLogsBox = new HBox(10);
                        chatLogsBox.setAlignment(Pos.CENTER);

                        // Label for conversation header
                        Label conversationHeader = new Label("--- Conversation with " + otherParticipant.getUserName() + " ---");

                        // VBox to hold the messages, wrapped in a ScrollPane
                        VBox chatLogs = new VBox(10);
                        for (Message msg : selectedConvo.getMessages()) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                            String formattedTimestamp = msg.getTimestamp().format(formatter);
                            String senderName = msg.getSender().equals(currentUser) ? "You" : otherParticipant.getUserName();
                            Label messageLabel = new Label(senderName + " [" + formattedTimestamp + "]: " + msg.getContent());
                            chatLogs.getChildren().add(messageLabel);
                        }

                        // Wrap the chatLogs VBox in a ScrollPane
                        ScrollPane scrollPane = new ScrollPane(chatLogs);
                        scrollPane.setFitToWidth(true);
                        scrollPane.setPrefHeight(400); // Adjust the height as needed to fit the layout

                        chatLogsBox.getChildren().add(scrollPane);

                        // Bottom horizontal box for text input and send button
                        HBox inputBox = new HBox(10);
                        inputBox.setAlignment(Pos.CENTER_LEFT);

                        // Text area for new message input
                        TextArea newMessageInput = new TextArea();
                        newMessageInput.setWrapText(true);
                        newMessageInput.setPrefHeight(50); // Set a fixed height for the text area

                        // Send message button
                        Button sendMessageButton = new Button("Send Message");

                        inputBox.getChildren().addAll(newMessageInput, sendMessageButton);

                        // Add the top chat logs and bottom input box to the vertical layout
                        conversationLayout.getChildren().addAll(conversationHeader, chatLogsBox, inputBox);

                        // Set action for send message button
                        sendMessageButton.setOnAction(sendEvent -> {
                            String messageContent = newMessageInput.getText();

                            if (!messageContent.isEmpty()) {
                                // Create and send the message
                                Message newMessage = new Message("1", currentUser, otherParticipant, messageContent);
                                boolean success = userServer.sendDirectMessage(newMessage, currentUser, otherParticipant);

                                newMessageInput.clear();
                    
                                // Dynamically add the new message to the chat log
                                String senderName = "You";
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                                String formattedTimestamp = newMessage.getTimestamp().format(formatter);
                                Label messageLabel = new Label(senderName + " [" + formattedTimestamp + "]: " + newMessage.getContent());
                                chatLogs.getChildren().add(messageLabel); // Update the chat log with the new message
                    
                                // Optionally, scroll to the bottom of the chat log
                                scrollPane.setVvalue(1.0); // This ensures the scroll pane moves to the latest message
                                if (!success) {
                                    Label statusLabel = new Label("Failed to send message to " + otherParticipant.getUserName());
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
