package com.psd;

import com.psd.entities.User;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

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
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
        sendUserToCentralServer(serializeUser(user));
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
            clientSocket = (SSLSocket) ssf.createSocket("127.0.0.1", 8888);

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
