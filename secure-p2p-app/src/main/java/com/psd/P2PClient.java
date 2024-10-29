package com.psd;

import com.psd.entities.User;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

public class P2PClient {

    private SSLSocket clientSocket;
    private SSLSocketFactory ssf;

    public P2PClient(User user) {
        try {
            // Setup SSL context with the keystore and truststore
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");

            // Load the keystore (adjust the path to your keystore)
            try (InputStream keyStoreStream = new FileInputStream("keystore.jks")) {
                ks.load(keyStoreStream, "psd2024".toCharArray());
            }

            // Initialize KeyManagerFactory with the keystore
            kmf.init(ks, "psd2024".toCharArray());

            // Load the truststore
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream trustStoreStream = new FileInputStream("truststore.jks")) {
                trustStore.load(trustStoreStream, "psd2024".toCharArray());
            }

            // Initialize TrustManagerFactory with the truststore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // Initialize the SSLContext with both key managers and trust managers
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // Create an SSLSocketFactory from the SSLContext
            ssf = sslContext.getSocketFactory();

            List<byte[]> uList = new ArrayList<>();
            uList.add(serializeUser(user));
            sendUserToCentralServer(uList);

        } catch (Exception e) {
            System.out.println("Error initializing P2PClient: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private byte[] serializeUser(User user) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(user);
            return byteOut.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static User deserializeUser(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(byteIn)) {
            return (User) in.readObject();
        }
    }

    private void sendUserToCentralServer(List<byte[]> users) {
        try {
            clientSocket = (SSLSocket) ssf.createSocket(InetAddress.getLocalHost(), 8888);

            DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());

            // Send the number of users (1 user = registration, 2 users = retrieval request)
            dataOut.writeInt(users.size());

            for (byte[] u : users) {
                // Send the length of the serialized user data
                dataOut.writeInt(u.length);
                // Send the serialized user data
                dataOut.write(u);
            }

            dataOut.flush();


        } catch (IOException e) {
            System.out.println("Error sending user to central server: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(User receiver, byte[] message) throws IOException {
        clientSocket = (SSLSocket) ssf.createSocket(receiver.getIpAddress(), receiver.getPort());

        // Initialize DataOutputStream with the socket's output stream
        DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());
        // Send the length of the message first
        dataOut.writeInt(message.length);

        // Send the serialized message to the connected peer
        dataOut.write(message);
        dataOut.flush();

    }

}
