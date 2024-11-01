package com.psd;

import com.psd.entities.User;
import com.psd.services.EncriptionService;

import javax.net.ssl.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class P2PClient {

    private User user;
    private SSLSocketFactory sslSocketFactory;
    private SSLSocket clientSocket;

    static {
        // Register the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Initializes the P2PClient by setting up an SSL context, creating an SSLSocketFactory, and sending the user data to the central server.
     *
     * @param user The User object to be serialized and sent to the central server.
     */
    public P2PClient(User user) {
        this.user = user;
        sslSocketFactory = createSSLSocketFactory(); // Initialize SSL context when client is created
        if (sslSocketFactory == null) {
            System.out.println("Failed to create SSL peer client socket factory.");
            return;
        }

        // Serialize the user object and send it to the central server
        List<byte[]> serializedUsers = new ArrayList<>();
        serializedUsers.add(serializeUser(user));
        sendUserToCentralServer(serializedUsers);
    }

    private SSLSocketFactory createSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");

            try (InputStream keyStoreStream = new FileInputStream(EncriptionService.getStoreDirectory() + this.user.getUserID() + "-keystore.jks")) {
                ks.load(keyStoreStream, "centralServer".toCharArray());
            }
            kmf.init(ks, "centralServer".toCharArray());

            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream trustStoreStream = new FileInputStream(EncriptionService.getStoreDirectory() + "server-truststore.jks")) {
                trustStore.load(trustStoreStream, "centralServer".toCharArray());
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext.getSocketFactory();

        } catch (Exception e) {
            System.out.println("Error creating SSL context: " + e.getMessage());
            return null;
        }
    }


    /**
     * Serializes a User object to a byte array.
     *
     * @param user The User object to be serialized.
     * @return A byte array representing the serialized user object.
     */
    private byte[] serializeUser(User user) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(user); // Serialize user object
            return byteOut.toByteArray();
        } catch (IOException e) {
            System.out.println("Error serializing user: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to serialize user", e);
        }
    }

    /**
     * Sends a list of serialized user data to the central server.
     *
     * @param users List of serialized user objects in byte array format.
     */
    public void sendUserToCentralServer(List<byte[]> users) {
        try {
            createSSLSocketFactory();
            // Connect to central server over SSL
            clientSocket = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getLocalHost(), 8888);

            // Set up output stream for sending data
            DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());

            // Send the number of user objects
            dataOut.writeInt(users.size());

            // Send each user object
            for (byte[] user : users) {
                dataOut.writeInt(user.length); // Send length of user data
                dataOut.write(user); // Send user data
            }

            dataOut.flush();

        } catch (IOException e) {
            System.out.println("Error connecting to central server: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to central server", e);
        }
    }

    /**
     * Sends a message to a specified user by connecting to their IP and port over SSL.
     *
     * @param receiver The User object representing the message receiver.
     * @param message  The message to be sent as a byte array.
     * @throws IOException if an I/O error occurs during message sending.
     */
    public void sendMessage(User receiver, byte[] message) {
        try {
            sslSocketFactory = createSSLSocketFactory();

            // Connect to the receiver's IP and port over SSL
            clientSocket = (SSLSocket) sslSocketFactory.createSocket(receiver.getIpAddress(), receiver.getPort());

            // Initialize output stream for sending message
            DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());
            dataOut.writeInt(message.length); // Send message length
            dataOut.write(message); // Send message data
            dataOut.flush();
        } catch (IOException e) {
            System.out.println("Error connecting to receiver: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to receiver", e);
        }
    }

}
