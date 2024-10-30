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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class P2PClient {

    private SSLSocket clientSocket;
    private SSLSocketFactory sslSocketFactory;

    static {
        // Register the Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    public P2PClient(User user) {
        sslSocketFactory = createSSLSocketFactory(user); // Initialize SSL context when client is created
        if (sslSocketFactory == null) {
            System.out.println("Failed to create SSL peer client socket factory.");
            return;
        }
        sendUserToCentralServer(serializeUser(user));
    }

    private static SSLSocketFactory createSSLSocketFactory(User user) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");

            try (InputStream keyStoreStream = new FileInputStream(EncriptionService.getStoreDirectory() + user.getUserName() + "-keystore.jks")) {
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

    public void sendMessage(User receiver, byte[] message) throws IOException {
        clientSocket = (SSLSocket) sslSocketFactory.createSocket(receiver.getIpAddress(), receiver.getPort());

        // Initialize DataOutputStream with the socket's output stream
        DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());
        // Send the length of the message first
        dataOut.writeInt(message.length);

        // Send the serialized message to the connected peer
        dataOut.write(message);
        dataOut.flush();
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

    private void sendUserToCentralServer(byte[] user)  {
        try {
            clientSocket = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getLocalHost(), 8888);
            System.out.println("ola");
            // Initialize DataOutputStream with the socket's output stream
            DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream());
            System.out.println("AQUI" + user.length);

            // Send the length of the message first
            dataOut.writeInt(user.length);

            // Send the serialized message to the connected peer
            dataOut.write(user);
            dataOut.flush();

        }catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

}
