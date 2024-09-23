package com.psd;

import com.psd.entities.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Represents a P2P server that listens for incoming messages from peers.
 * Each user has their own P2PServer running, which listens on the specified port.
 */
public class P2PServer {

    private int port;  // The port on which the server listens
    private User currentUser;  // The user running this server

    public P2PServer(int port, User currentUser) {
        this.port = port;
        this.currentUser = currentUser;
    }

    /**
     * Starts the server to listen for incoming messages on the specified port.
     */
    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println(currentUser.getUserID() + " is listening on port " + port);

        // Continuously listen for incoming connections
        while (true) {
            try {
                Socket socket = serverSocket.accept();  // Accept incoming connection
                new Thread(new ClientHandler(socket)).start();  // Handle each client in a new thread
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message = reader.readLine();
                System.out.println("Received message: " + message);

                // You can add more logic here to handle the received message,
                // for example, storing it, responding to the peer, etc.

                socket.close();  // Close the connection after the message is received
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
 