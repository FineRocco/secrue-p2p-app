package com.psd.services;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class EncryptionService {

    private static final String ALGORITHM = "AES";

    /**
     * Encrypts a given object using the provided secret key.
     *
     * @param secretKey The secret key (Base64-encoded) used for encryption.
     * @param object    The object to be encrypted.
     * @return The encrypted object as a Base64-encoded string.
     * @throws GeneralSecurityException If an error occurs during encryption.
     * @throws IOException              If an error occurs during serialization.
     */
    public static String encryptObject(BigInteger secretKey, Object object) throws GeneralSecurityException, IOException {
        // Convert Base64-encoded key to SecretKey
        byte[] decodedKey = secretKey.toByteArray();
        SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);

        // Serialize the object to bytes
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
            objectStream.writeObject(object);

        }

        byte[] serializedData = byteStream.toByteArray();

        // Encrypt the serialized data
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedData = cipher.doFinal(serializedData);

        // Return the encrypted data as a Base64-encoded string
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * Decrypts a given Base64-encoded string into an object using the provided secret key.
     *
     * @param secretKey       The secret key (Base64-encoded) used for decryption.
     * @param encryptedObject The encrypted object as a Base64-encoded string.
     * @return The decrypted object.
     * @throws GeneralSecurityException If an error occurs during decryption.
     * @throws IOException              If an error occurs during deserialization.
     * @throws ClassNotFoundException   If the object's class is not found.
     */
    public static Object decryptObject(BigInteger secretKey, String encryptedObject) throws GeneralSecurityException, IOException, ClassNotFoundException {
        // Convert Base64-encoded key to SecretKey
        byte[] decodedKey = secretKey.toByteArray();
        SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);

        // Decode the Base64-encoded encrypted object
        byte[] encryptedData = Base64.getDecoder().decode(encryptedObject);

        // Decrypt the data
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedData = cipher.doFinal(encryptedData);

        // Deserialize the object from bytes
        try (ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(decryptedData))) {
            return objectStream.readObject();
        }
    }
}
