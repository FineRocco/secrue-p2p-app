package com.psd.services;

import com.psd.entities.Share;

import java.math.BigInteger;
import java.security.SecureRandom;

public class SecretSharingService {

    private static final int N_SHARE_HOLDERS = 3; // Number of shares to generate
    private static final int POLY_DEGREE = 1;   // Minimum shares required to reconstruct the key
    private static final BigInteger field = new BigInteger("8CF83642A709A097B447997640129DA299B1A47D1EB3750BA308B0FE64F5FBD3", 16);
    private static final SecureRandom rndGenerator = new SecureRandom();

    /**
     * Generates a random key, encrypts it, and splits it into shares using Shamir's Secret Sharing.
     *
     * @return A map where each key is a share index (integer) and the value is the Base64-encoded share.
     */
    public static BigInteger generateKey() {
        return new BigInteger(field.bitLength() - 1, rndGenerator);
    }

    /**
     * This method shares a secret using Shamir's scheme.
     *
     * @param secret Secret to share.
     * @return Shares of the secret.
     */
    public static Share[] shares(BigInteger secret) {
        //creating polynomial: P(x) = a_d * x^d + ... + a_1 * x^1 + secret
        BigInteger[] polynomial = new BigInteger[POLY_DEGREE + 1];

        //TO COMPLETE: DONE
        polynomial[0] = secret;

        for (int i = 1; i < polynomial.length; i++) {
            polynomial[i] = new BigInteger(field.bitLength() - 1, rndGenerator);
        }

        //calculating shares
        Share[] shares = new Share[N_SHARE_HOLDERS];
        for (int i = 0; i < N_SHARE_HOLDERS; i++) {
            BigInteger shareholder = BigInteger.valueOf(i + 1); //shareholder id can be any positive number, except 0
            BigInteger share = calculatePoint(shareholder, polynomial);
            shares[i] = new Share(shareholder, share);
        }

        return shares;
    }

    /**
     * This method combines shares, using Lagrange polynomials, to recover the secret.
     * Lagrange polynomials: https://en.wikipedia.org/wiki/Lagrange_polynomial.
     *
     * @param shares Shares of the secret.
     * @return Recovered secret.
     */
    public static BigInteger combine(Share[] shares) {
        BigInteger secret = BigInteger.ZERO;

        for (int j = 0; j < shares.length; j++) {
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int m = 0; m < shares.length; m++) {

                if (m != j) {

                    numerator = numerator.multiply(shares[m].getShareholder()).mod(field);
                    denominator = denominator.multiply(shares[m].getShareholder().subtract(shares[j].getShareholder())).mod(field);
                }
            }

            BigInteger lagrange = numerator.multiply(denominator.modInverse(field)).mod(field);

            secret = secret.add(shares[j].getShare().multiply(lagrange)).mod(field);
        }
        return secret;
    }

    /**
     * This method calculates a point on a polynomial using the Horner's method:
     * https://en.wikipedia.org/wiki/Horner%27s_method.
     *
     * @param x          X value.
     * @param polynomial Polynomial P(x).
     * @return Y value.
     */
    private static BigInteger calculatePoint(BigInteger x, BigInteger[] polynomial) {
        BigInteger b = polynomial[polynomial.length - 1];

        //TO COMPLETE: DONE
        for (int i = polynomial.length - 2; i >= 0; i--) {

            b = polynomial[i].add(b.multiply(x)).mod(field);

        }
        return b;
    }
}
