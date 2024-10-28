package com.psd;

import com.psd.entities.Conversation;
import com.psd.entities.Message;
import com.psd.entities.User;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Map;

public class CentralServer {
    static Map<String, User> userRegistry; // Stores username and associated user info
    private static volatile boolean isRunning = false;
    public static void main(String[] args) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8888,50, InetAddress.getByName("localhost"))) {
                System.out.println("Central Server started on port: " + 8888);
                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    new ClientHandler(clientSocket).run();
                }
            } catch (IOException e) {
                System.out.println("Error starting server: " + e.getMessage());
            }
        }).start();
    }
    // Registers a new user to the central server
    public static synchronized void registerUser(User user) {
        userRegistry.put(user.getUserName(), user);
        System.out.println("User registered: " + user.getUserName());
    }

    // Fetches a user from the registry based on username
    public static synchronized User getUser(String username) {
        return userRegistry.get(username);
    }

    private static byte[] serializeMessage(User user) throws IOException {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(user);
            return byteOut.toByteArray();
        }
    }

    private static User deserializeMessage(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(byteIn)) {
            return (User) in.readObject();
        }
    }

    /**
     * Handles the communication with an individual client (peer) in a separate thread.
     */
    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;

        }

        @Override
        public void run() {
            try {
                // Read the message sent by the peer
                DataInputStream dataIn = new DataInputStream(socket.getInputStream());
                // Create an SSLSocketFactory from the SSLContext

                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());

                // Read the length of the incoming message
                int messageLength = dataIn.readInt();

                // Initialize a byte array to hold the exact message size
                byte[] messageBytes = new byte[messageLength];

                // Read the message into the byte array
                int totalBytesRead = 0;
                while (totalBytesRead < messageLength) {
                    int bytesRead = dataIn.read(messageBytes, totalBytesRead, messageLength - totalBytesRead);
                    if (bytesRead == -1) break; // End of stream
                    totalBytesRead += bytesRead;
                }

                // Deserialize the message
                User user = deserializeMessage(messageBytes);
                if(user.getIpAddress().equals(null) && user.getPort()==0){
                    User u = getUser(user.getUserName());
                    byte[] message = serializeMessage(u);
                    dataOut.writeInt(message.length);
                    dataOut.write(message);
                }else{
                    registerUser(user);
                }

                socket.close();  // Close the connection after the message is received

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }
}

