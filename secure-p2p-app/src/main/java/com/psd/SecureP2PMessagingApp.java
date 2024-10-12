package com.psd;

import com.psd.entities.*;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
        Button addContactButton = new Button("Add Contact");
        Button seeContactsButton = new Button("See your Contacts");
        Button sendContMessageButton = new Button("Send Message");
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
                sideBar.getChildren().addAll(conversationsButton, sendContMessageButton, sendDirectMessageButton, addContactButton, seeContactsButton, exitButton);

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

        sendContMessageButton.setOnAction(event -> {
            // Create a layout to display the contacts and message input
            VBox sendMessageLayout = new VBox(10);
            sendMessageLayout.setAlignment(Pos.CENTER);

            // Label to display contacts
            Label contactsLabel = new Label("Your Contacts:");
            sendMessageLayout.getChildren().add(contactsLabel);

            // Fetch and display the contacts
            ComboBox<String> contactComboBox = new ComboBox<>();
            for (User contact : currentUser.getContacts()) {
                contactComboBox.getItems().add(contact.getUserName());
            }

            // Input fields for message
            Label messageLabel = new Label("Enter your message:");
            TextArea messageInput = new TextArea();
            messageInput.setWrapText(true);
            Button sendButton = new Button("Send");

            // Add all elements to the layout
            sendMessageLayout.getChildren().addAll(contactComboBox, messageLabel, messageInput, sendButton);

            // Set the new layout to the center pane of the BorderPane
            mainMenuLayout.setCenter(sendMessageLayout);

            // Set the action for the send button
            sendButton.setOnAction(sendEvent -> {
                String receiverName = contactComboBox.getValue();
                String messageContent = messageInput.getText();

                if (receiverName != null && !messageContent.isEmpty()) {
                    // Find the peer in contacts
                    User receiver = currentUser.findContactByUsername(receiverName);

                    if (receiver != null) {
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
                        // Handle case where the contact is not found
                        Label errorLabel = new Label("Peer not found.");
                        mainMenuLayout.setCenter(new StackPane(errorLabel));
                    }
                } else {
                    // Handle case where no contact or message is provided
                    if (receiverName == null) {
                        contactComboBox.setPromptText("Select a contact");
                    }
                    if (messageContent.isEmpty()) {
                        messageInput.setPromptText("Enter a message");
                    }
                }
            });
        });

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
        
                        // Create a layout to display the conversation
                        VBox conversationDetailsLayout = new VBox(10);
                        conversationDetailsLayout.setAlignment(Pos.CENTER);
        
                        Label conversationHeader = new Label("--- Conversation with " + otherParticipant.getUserName() + " ---");
                        conversationDetailsLayout.getChildren().add(conversationHeader);
        
                        // Display messages in the conversation
                        for (Message msg : selectedConvo.getMessages()) {
                            String senderName = msg.getSender().equals(currentUser) ? "You" : otherParticipant.getUserName();
                            Label messageLabel = new Label(senderName + " [" + msg.getTimestamp() + "]: " + msg.getContent());
                            conversationDetailsLayout.getChildren().add(messageLabel);
                        }
        
                        // Input for new message
                        TextArea newMessageInput = new TextArea();
                        newMessageInput.setWrapText(true);
                        Button sendMessageButton = new Button("Send Message");
        
                        conversationDetailsLayout.getChildren().addAll(newMessageInput, sendMessageButton);
        
                        // Set action for send message button
                        sendMessageButton.setOnAction(sendEvent -> {
                            String messageContent = newMessageInput.getText();
        
                            if (!messageContent.isEmpty()) {
                                // Create and send the message
                                Message newMessage = new Message("1", currentUser, otherParticipant, messageContent);
                                boolean success = userServer.sendDirectMessage(newMessage, currentUser, otherParticipant);
        
                                Label statusLabel;
                                if (success) {
                                    statusLabel = new Label("Message sent to " + otherParticipant.getUserName());
                                    newMessageInput.clear();
        
                                    // Update conversation with the new message
                                    selectedConvo.addMessage(newMessage);
                                    String senderName = "You";
                                    Label messageLabel = new Label(senderName + " [" + newMessage.getTimestamp() + "]: " + newMessage.getContent());
                                    conversationDetailsLayout.getChildren().add(conversationDetailsLayout.getChildren().size() - 2, messageLabel);
                                } else {
                                    statusLabel = new Label("Failed to send message to " + otherParticipant.getUserName());
                                }
        
                                conversationDetailsLayout.getChildren().add(statusLabel);
                            }
                        });
        
                        // Set the new layout to the center pane of the BorderPane
                        mainMenuLayout.setCenter(conversationDetailsLayout) ;
                    } else {
                        conversationComboBox.setPromptText("Select a conversation");
                    }
                });
            }
        
            // Set the conversations layout to the center pane
            mainMenuLayout.setCenter(conversationsLayout);
        });
        

        addContactButton.setOnAction(event -> {
            // Create the input fields and labels for adding a contact
            Label contactUsernameLabel = new Label("Enter the contact's username:");
            TextField contactUsernameInput = new TextField();
        
            Label contactIpLabel = new Label("Enter the contact's IP address:");
            TextField contactIpInput = new TextField();
        
            Label contactPortLabel = new Label("Enter the contact's port:");
            TextField contactPortInput = new TextField();
        
            Button addContactSubmitButton = new Button("Add Contact");
        
            // Create a new layout for the center pane to add contact details
            VBox addContactLayout = new VBox(10);
            addContactLayout.setAlignment(Pos.CENTER);
            addContactLayout.getChildren().addAll(contactUsernameLabel, contactUsernameInput, contactIpLabel, contactIpInput, contactPortLabel, contactPortInput, addContactSubmitButton);
        
            // Set the new center pane to the BorderPane
            mainMenuLayout.setCenter(addContactLayout);
        
            // Set the action for the add contact submit button
            addContactSubmitButton.setOnAction(submitEvent -> {
                try {
                    // Extract the entered contact details
                    String contactUsername = contactUsernameInput.getText();
                    String contactIp = contactIpInput.getText();
                    int contactPort = Integer.parseInt(contactPortInput.getText());
        
                    // Create a new User object for the contact
                    User newContact = new User(contactUsername, null, null, contactIp, contactPort);
        
                    // Add the new contact to the current user's contact list
                    currentUser.addContact(newContact);
        
                    // Confirmation message
                    Label confirmationLabel = new Label("Contact " + contactUsername + " added successfully.");
        
                    // Update the center pane with the confirmation message
                    mainMenuLayout.setCenter(new StackPane(confirmationLabel));
        
                } catch (NumberFormatException ex) {
                    // Handle invalid port input
                    contactPortInput.setText("Enter a valid port number");
                }
            });
        });

        seeContactsButton.setOnAction(event -> {
            // Create a layout to display the contacts
            VBox contactsLayout = new VBox(10);
            contactsLayout.setAlignment(Pos.CENTER);
        
            // Label for contacts section
            Label contactsLabel = new Label("Your Contacts:");
            contactsLayout.getChildren().add(contactsLabel);
        
            // Fetch and display the contacts
            for (User contact : currentUser.getContacts()) {
                Label contactLabel = new Label("Username: " + contact.getUserName() + ", IP: " + contact.getIpAddress() + ", Port: " + contact.getPort());
                contactsLayout.getChildren().add(contactLabel);
            }
        
            // Set the new layout to the center pane of the BorderPane
            mainMenuLayout.setCenter(contactsLayout);
        });

        exitButton.setOnAction(event -> {
            System.out.println("Exiting...");
            primaryStage.close(); // Close the primary stage to exit the application
            System.exit(0); // Ensures the program is terminated properly
        });
        
    }

    
}
