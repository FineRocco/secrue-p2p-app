package com.psd.services;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.math.BigInteger;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Signature;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.ContentSigner;

public class EncriptionService {

    // Define the directory for all keystore and truststore files
    public static final String STORE_DIRECTORY = "C:\\Users\\l3tim\\Desktop\\PSD\\secrue-p2p-app\\secrue-p2p-app\\secure-p2p-app\\src\\main\\java\\com\\psd\\stores\\";

    public static String getStoreDirectory() {
        return STORE_DIRECTORY;
    }

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    public static X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + 365L * 24 * 60 * 60 * 1000); // Valid for 1 year

        X500Principal dnName = new X500Principal("CN=Peer Certificate");
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        
        // Create the certificate holder
        X509CertificateHolder certificateHolder = new JcaX509v3CertificateBuilder(
                dnName, certSerialNumber, startDate, endDate, dnName, keyPair.getPublic())
                .build(contentSigner);

        // Convert to X509Certificate using JcaX509CertificateConverter
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder);
    }

    public static void saveKeyStore(KeyPair keyPair, X509Certificate cert, String peerId) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null); // Initialize empty keystore
    
        // Store the private key and the certificate in the keystore
        keyStore.setKeyEntry(peerId, keyPair.getPrivate(), "centralServer".toCharArray(), new Certificate[]{cert});
    
        // Save the keystore to file
        try (FileOutputStream fos = new FileOutputStream(STORE_DIRECTORY + peerId + "-keystore.jks")) {
            keyStore.store(fos, "centralServer".toCharArray());
        }
    }

    public static void importCertToTruststore(X509Certificate cert, String truststoreName, String peer) throws Exception {
        KeyStore truststore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(STORE_DIRECTORY + truststoreName)) {
            truststore.load(fis, "centralServer".toCharArray());
        }

        // Add certificate to truststore
        truststore.setCertificateEntry(peer, cert);

        try (FileOutputStream fos = new FileOutputStream(STORE_DIRECTORY + truststoreName)) {
            truststore.store(fos, "centralServer".toCharArray());
        }
    }

    public static void createServerTrustStore(X509Certificate cert) throws Exception {
        KeyStore truststore = KeyStore.getInstance("JKS");
        truststore.load(null, null); // Initialize empty truststore

        // Add server certificate to the truststore
        truststore.setCertificateEntry("server-cert", cert);

        // Save the truststore to a file
        try (FileOutputStream fos = new FileOutputStream(STORE_DIRECTORY + "server-truststore.jks")) {
            truststore.store(fos, "centralServer".toCharArray());
        }

        System.out.println("Truststore created successfully: server-truststore.jks");
    }

    
}
