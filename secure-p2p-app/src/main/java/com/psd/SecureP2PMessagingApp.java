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
 * Initializes services and simulates sending encrypted messages between users.
 */
public class SecureP2PMessagingApp {

    public static void main(String[] args) throws IOException{

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
        User currentUser = new User(userName,publicKey, privateKey,"127.0.0.1");
        userNetwork.connectPeer(currentUser);

        System.out.println(userName + " is now online. Listening on port " + userPort);

        System.out.println("Choose receiver's port to send the message");
        int receiverPort = Integer.parseInt(bufferedReader.readLine());



        // Allow user to send a message to Bob
        while (true) {
            System.out.print("Enter a message to send: ");
            String messageContent = scanner.nextLine();
            
            if (messageContent.equalsIgnoreCase("exit")) {
                break;  // Exit the loop and stop messaging
            }

            Message message = new Message("1", currentUser, currentUser, messageContent);
            userNetwork.sendDirectMessage(message, currentUser, currentUser, receiverPort); // Send the message to Bob

        }

        System.out.println("Exiting chat...");
        scanner.close();
    }
        

}

