package com.psd.services;

import com.codahale.shamir.Scheme;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class SecretSharingService {

    private static final int TOTAL_SHARES = 3; // Number of shares to generate
    private static final int THRESHOLD = 2;   // Minimum shares required to reconstruct the key

    /**
     * Generates a random key, encrypts it, and splits it into shares using Shamir's Secret Sharing.
     *
     * @return A map where each key is a share index (integer) and the value is the Base64-encoded share.
     */
    
    public static Map<Integer, String> generateAndSplitKey() {
        try {
            // Generate a random 256-bit AES key using KeyGenerator
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256); // Key size: 256 bits
            SecretKey secretKey = keyGenerator.generateKey();
            byte[] key = secretKey.getEncoded();
            System.out.println("Generated Key: " + Base64.getEncoder().encodeToString(key));
    
            // Initialize Shamir's Secret Sharing Scheme
            Scheme scheme = new Scheme(new java.security.SecureRandom(), TOTAL_SHARES, THRESHOLD);
    
            // Split the key into shares
            Map<Integer, byte[]> shares = scheme.split(key);
    
            // Encode each share to Base64 for safe transmission/storage
            Map<Integer, String> encodedShares = shares.entrySet().stream()
                    .collect(
                            java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> Base64.getEncoder().encodeToString(e.getValue())
                            )
                    );
    
            // Print the shares for verification
            encodedShares.forEach((index, share) -> System.out.println("Share " + index + ": " + share));
    
            return encodedShares;
        } catch (Exception e) {
            throw new RuntimeException("Error generating or splitting key: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reconstructs the original key from the shares.
     *
     * @param shares The map of shares (key: share index, value: share data as Base64 string).
     * @return The original key as a string.
     */
    public static String reconstructKey(Map<Integer, String> shares) {
        try {
            // Decode shares from Base64
            Map<Integer, byte[]> decodedShares = shares.entrySet().stream()
                    .collect(
                            java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> Base64.getDecoder().decode(e.getValue())
                            )
                    );

            // Use Shamir's Secret Sharing to reconstruct the key
            Scheme scheme = new Scheme(new java.security.SecureRandom(), TOTAL_SHARES, THRESHOLD);
            byte[] keyBytes = scheme.join(decodedShares);

            // Return the reconstructed key as a Base64 string
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error reconstructing key: " + e.getMessage(), e);
        }
    }

}
