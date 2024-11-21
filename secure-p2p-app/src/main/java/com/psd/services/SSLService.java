/**
 * The {@code EncriptionService} class provides utilities for generating and managing
 * cryptographic key pairs, certificates, keystores, and SSL context configuration.
 * This service facilitates secure communication using SSL/TLS by generating and storing
 * key pairs, self-signed certificates, and managing trust stores for server and client
 * authentication.
 *
 * <p>Main functionalities include:
 * <ul>
 *   <li>Generating RSA key pairs and self-signed X.509 certificates</li>
 *   <li>Storing keys and certificates in Java KeyStores (JKS format)</li>
 *   <li>Creating and configuring SSL/TLS contexts for secure communication</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 *     KeyPair keyPair = EncriptionService.generateKeyPair();
 *     X509Certificate cert = EncriptionService.generateSelfSignedCertificate(keyPair);
 *     EncriptionService.saveKeyStore(keyPair, cert, "peerId");
 * </pre>
 *
 * <p>Note: This class uses the BouncyCastle library for certificate generation.
 */
package com.psd.services;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.math.BigInteger;
import java.util.Date;
import java.util.Objects;

import javax.security.auth.x500.X500Principal;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.net.ssl.*;
import java.io.*;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.ContentSigner;

public class SSLService {

    // Directory for storing keystore and truststore files
    public static final String STORE_DIRECTORY = "secure-p2p-app\\src\\main\\java\\com\\psd\\stores\\";

    /**
     * Retrieves the store directory.
     *
     * @return The directory path for keystore and truststore files.
     */
    public static String getStoreDirectory() {
        return STORE_DIRECTORY;
    }

    /**
     * Generates a new RSA key pair.
     *
     * @return A newly generated RSA {@link KeyPair}.
     * @throws Exception if key generation fails.
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    /**
     * Generates a self-signed X.509 certificate using the provided key pair.
     *
     * @param keyPair The {@link KeyPair} used for generating the certificate.
     * @return A self-signed {@link X509Certificate}.
     * @throws Exception if certificate generation fails.
     */
    public static X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + 365L * 24 * 60 * 60 * 1000); // Valid for 1 year

        X500Principal dnName = new X500Principal("CN=Peer Certificate");
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());

        X509CertificateHolder certificateHolder = new JcaX509v3CertificateBuilder(
                dnName, certSerialNumber, startDate, endDate, dnName, keyPair.getPublic())
                .build(contentSigner);

        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder);
    }

    /**
     * Saves a key pair and certificate in a keystore file.
     *
     * @param keyPair The {@link KeyPair} to be saved.
     * @param cert The {@link X509Certificate} associated with the key pair.
     * @param peerId The identifier for the peer, used as an alias in the keystore.
     * @throws Exception if keystore saving fails.
     */
    public static void saveKeyStore(KeyPair keyPair, X509Certificate cert, String peerId) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        keyStore.setKeyEntry(peerId, keyPair.getPrivate(), "centralServer".toCharArray(), new Certificate[]{cert});

        try (FileOutputStream fos = new FileOutputStream(STORE_DIRECTORY + peerId + "-keystore.jks")) {
            keyStore.store(fos, "centralServer".toCharArray());
        }
    }

    /**
     * Initializes server keys by generating a key pair and self-signed certificate,
     * saving them to a keystore, and importing the certificate into the truststore.
     *
     * @param peerId The identifier for the server peer.
     */
    public static void initializeServerKeys(String peerId) {
        try {
            KeyPair keyPair = generateKeyPair();
            X509Certificate cert = generateSelfSignedCertificate(keyPair);
            saveKeyStore(keyPair, cert, peerId);
            importCertToTruststore(cert, peerId);
        } catch (Exception e) {
            System.out.println("Error generating server keys and certificate: " + e.getMessage());
        }
    }

    /**
     * Imports a certificate into the truststore.
     *
     * @param cert The {@link X509Certificate} to be imported.
     * @param peer The alias for the certificate entry in the truststore.
     * @throws Exception if the truststore operation fails.
     */
    public static void importCertToTruststore(X509Certificate cert, String peer) throws Exception {
        KeyStore truststore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(STORE_DIRECTORY + "server-truststore.jks")) {
            truststore.load(fis, "centralServer".toCharArray());
        }

        truststore.setCertificateEntry(peer, cert);

        try (FileOutputStream fos = new FileOutputStream(STORE_DIRECTORY + "server-truststore.jks")) {
            truststore.store(fos, "centralServer".toCharArray());
        }
    }

    /**
     * Creates a truststore and adds the provided certificate.
     *
     * @param cert The {@link X509Certificate} to be added to the truststore.
     * @throws Exception if truststore creation fails.
     */
    public static void createServerTrustStore(X509Certificate cert) throws Exception {
        KeyStore truststore = KeyStore.getInstance("JKS");
        truststore.load(null, null);

        truststore.setCertificateEntry("server", cert);

        try (FileOutputStream fos = new FileOutputStream(STORE_DIRECTORY + "server-truststore.jks")) {
            truststore.store(fos, "centralServer".toCharArray());
        }

        System.out.println("Truststore created successfully: server-truststore.jks");
    }

    /**
     * Initializes the server's truststore by adding the server's certificate.
     */
    public static void initializeServerTruststore() {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream keyStoreStream = new FileInputStream(STORE_DIRECTORY + "server-keystore.jks")) {
                keyStore.load(keyStoreStream, "centralServer".toCharArray());
            }

            X509Certificate serverCert = (X509Certificate) keyStore.getCertificate("server");

            createServerTrustStore(serverCert);
        } catch (Exception e) {
            System.out.println("Error generating server truststore: " + e.getMessage());
        }
    }

    /**
     * Initializes an SSL context for a client using its keystore and the server's truststore.
     *
     * @param username The username identifying the client's keystore.
     * @return An {@link SSLSocketFactory} configured for secure client communication.
     */
    public static SSLSocketFactory initializeSSLContext(String username) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = initializeKeyManagerFactory(username);
            TrustManagerFactory tmf = initializeTrustManagerFactory();

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            System.out.println("Error creating server SSL context: " + e.getMessage());
            return null;
        }
    }

    /**
     * Initializes an SSL context for the server using its keystore and the truststore.
     *
     * @param username The username identifying the server's keystore.
     * @return An {@link SSLServerSocketFactory} configured for secure server communication.
     */
    public static SSLServerSocketFactory initializeServerSSLContext(String username) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmf = initializeKeyManagerFactory(username);
            TrustManagerFactory tmf = initializeTrustManagerFactory();

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext.getServerSocketFactory();
        } catch (Exception e) {
            System.out.println("Error creating server SSL context: " + e.getMessage());
            return null;
        }
    }

    /**
     * Initializes a KeyManagerFactory using the specified keystore.
     *
     * @param username The identifier for the keystore.
     * @return A configured {@link KeyManagerFactory}.
     */
    public static KeyManagerFactory initializeKeyManagerFactory(String username) {
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");

            try (InputStream keyStoreStream = new FileInputStream(STORE_DIRECTORY + username + "-keystore.jks")) {
                ks.load(keyStoreStream, "centralServer".toCharArray());
            }
            kmf.init(ks, "centralServer".toCharArray());
            return kmf;
        } catch (Exception e) {
            System.out.println("Error initializing Key Manager Factory: " + e.getMessage());
            return null;
        }
    }

    /**
     * Initializes a TrustManagerFactory using the server's truststore.
     *
     * @return A configured {@link TrustManagerFactory}.
     */
    public static TrustManagerFactory initializeTrustManagerFactory() {
        try {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream trustStoreStream = new FileInputStream(STORE_DIRECTORY + "server-truststore.jks")) {
                trustStore.load(trustStoreStream, "centralServer".toCharArray());
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            return tmf;
        } catch (Exception e) {
            System.out.println("Error creating server SSL context: " + e.getMessage());
            return null;
        }
    }

    // ------------------ Group Methods ------------------ //

    /**
     * Creates an empty truststore for a specified group.
     *
     * @param trustStoreName The name of the truststore file.
     */
    public static void createGroupTruststore(String trustStoreName) {
        try {
            KeyStore truststore = KeyStore.getInstance("JKS");
            truststore.load(null, null);  // Create an empty truststore

            try (FileOutputStream fos = new FileOutputStream(STORE_DIRECTORY + trustStoreName + "-truststore.jks")) {
                truststore.store(fos, "centralServer".toCharArray());
                System.out.println("Created empty truststore: " + trustStoreName);
            }
        } catch (Exception e) {
            System.out.println("Error creating truststore " + trustStoreName + ": " + e.getMessage());
        }
    }

    /**
     * Imports a user's certificate from their keystore into the specified group truststore.
     *
     * @param groupName The group name (football, ufc, basketball) to select the corresponding truststore.
     * @param userId The user's ID to use as an alias in the truststore.
     * @throws Exception if an error occurs during the import process.
     */
    public static void importUserCertificateToGroupTruststore(String groupName, String userId) {
        String userKeystorePath = STORE_DIRECTORY + userId + "-keystore.jks";
        String groupTruststorePath = STORE_DIRECTORY + groupName.toLowerCase() + "-truststore.jks";
        
        try (FileInputStream userKeystoreStream = new FileInputStream(userKeystorePath);
             FileInputStream groupTruststoreStream = new FileInputStream(groupTruststorePath)) {

            // Load user's keystore
            KeyStore userKeystore = KeyStore.getInstance("JKS");
            userKeystore.load(userKeystoreStream, "centralServer".toCharArray());

            // Load the user's certificate from the keystore
            Certificate userCertificate = userKeystore.getCertificate(userId);
            Objects.requireNonNull(userCertificate, "User certificate not found in keystore.");

            // Load the group truststore
            KeyStore groupTruststore = KeyStore.getInstance("JKS");
            groupTruststore.load(groupTruststoreStream, "centralServer".toCharArray());

            // Add the user's certificate to the group truststore
            groupTruststore.setCertificateEntry(userId, userCertificate);

            // Save the updated truststore back to disk
            try (FileOutputStream fos = new FileOutputStream(groupTruststorePath)) {
                groupTruststore.store(fos, "centralServer".toCharArray());
                System.out.println("Added user certificate to " + groupName + " truststore: " + userId);
            }
        } catch (Exception e) {
            System.out.println("Error importing certificate for user " + userId + " to " + groupName + " truststore: " + e.getMessage());
        }
    }

}
