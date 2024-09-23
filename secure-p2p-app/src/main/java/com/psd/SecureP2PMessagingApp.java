package com.psd;

import com.psd.entities.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Scanner;

/**
 * Main class for the Secure P2P Messaging Application.
 * Initializes services and provides a terminal-based interface for interaction.
 */
public class SecureP2PMessagingApp {

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        PublicKey publicKey = null;
        PrivateKey privateKey = null;

        try {
            // Generate a temporary key pair (RSA algorithm)
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048); // You can set the key size (e.g., 2048 bits)
            
            // Generate the key pair
            KeyPair keyPair = keyPairGen.generateKeyPair();
            
            // Extract the public and private keys
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
            
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Ask the user for their name
        System.out.print("Enter your name: ");
        String userName = bufferedReader.readLine();

        // Ask the user to choose a port 
        System.out.print("Enter your port: ");
        int userPort = Integer.parseInt(bufferedReader.readLine());

        // Initialize the P2P network for user
        P2PNetwork userNetwork = new P2PNetwork(userPort);
        User currentUser = new User(userName, publicKey, privateKey, "127.0.0.1", userPort);
        userNetwork.connectPeer(currentUser);

        // Start the terminal interface loop
        boolean running = true;
        while (running) {
            // Display menu
            System.out.println("\n--- P2P Messaging App Menu ---");
            System.out.println("1. Send a message");
            System.out.println("2. Add a contact");
            System.out.println("3. View your contacts");
            System.out.println("4. Disconnect from the network");
            System.out.println("5. Exit");

            System.out.print("Choose an option: ");

            String option = bufferedReader.readLine();
            switch (option) {
                case "1":
                    // Sending a message
                    // Display contacts
                    currentUser.showContacts();
                    System.out.print("Enter peer's name: ");
                    String receiverName = bufferedReader.readLine();

                    // Find the peer in contacts
                    User receiver = currentUser.findContactByUsername(receiverName);

                    if (receiver != null) {
                        System.out.print("Enter your message: ");
                        String messageContent = bufferedReader.readLine();

                        // Create and send the message
                        Message message = new Message("1", currentUser, receiver, messageContent);
                        boolean success = userNetwork.sendDirectMessage(message, currentUser, receiver, receiver.getPort());

                        if (success) {
                            System.out.println("Message sent to " + receiverName);
                        } else {
                            System.out.println("Failed to send message to " + receiverName);
                        }
                    } else {
                        System.out.println("Peer not found.");
                    }
                    break;
                
                case "2":
                    //Add contact
                    System.out.print("Enter the contact's username: ");
                    String username = bufferedReader.readLine();
            
                    System.out.print("Enter the contact's IP address: ");
                    String ipAddress = bufferedReader.readLine();
            
                    System.out.print("Enter the contact's port: ");
                    int port = Integer.parseInt(bufferedReader.readLine());
            
                    // Create a new User object for the contact
                    User newContact = new User(username, null, null, ipAddress, port);
            
                    // Add the new contact to the current user's contact list
                    currentUser.addContact(newContact);
            
                    System.out.println("Contact " + username + " added successfully.");
                    break;

                case "3":
                    // Show contacts
                    currentUser.showContacts();
                    break;

                case "4":
                    userNetwork.disconnectPeer(currentUser);
                    System.out.println("You have been disconnected.");
                    running = false;
                    break;

                case "5":
                    System.out.println("Exiting...");
                    running = false;
                    scanner.close();
                    System.exit(0);
                    break;

                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }

        scanner.close();
    }
}
