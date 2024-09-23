package com.psd;

import java.io.*;
import java.net.*;

public class P2PClient {

    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    public P2PClient(String peerAddress, int peerPort) throws IOException {
        // Connect to the server (peer) on the specified IP address and port
        clientSocket = new Socket(peerAddress, peerPort);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    public void sendMessage(String message) {
        // Send message to the connected peer
        out.println(message);
    }

    public void receiveMessages() throws IOException {
        // Listen for messages from the peer
        String receivedMessage;
        while ((receivedMessage = in.readLine()) != null) {
            System.out.println("Received: " + receivedMessage);
        }
    }
}
