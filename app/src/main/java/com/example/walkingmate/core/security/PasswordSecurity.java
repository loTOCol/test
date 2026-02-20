package com.example.walkingmate.core.security;

import android.util.Base64;

import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordSecurity {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final String PREFIX = "pbkdf2$";

    private PasswordSecurity() {
    }

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return "";
        }
        if (isHashedPassword(plainPassword)) {
            return plainPassword;
        }
        try {
            byte[] salt = new byte[SALT_LENGTH];
            new SecureRandom().nextBytes(salt);
            byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
            String hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP);
            return PREFIX + ITERATIONS + "$" + saltB64 + "$" + hashB64;
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    public static boolean verifyPassword(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null) {
            return false;
        }
        if (!isHashedPassword(storedPassword)) {
            // Legacy fallback: support old plaintext accounts.
            return plainPassword.equals(storedPassword);
        }
        try {
            String[] parts = storedPassword.split("\\$");
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(parts[3], Base64.NO_WRAP);

            byte[] candidateHash = pbkdf2(plainPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
            return constantTimeEquals(expectedHash, candidateHash);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isHashedPassword(String password) {
        if (password == null) {
            return false;
        }
        if (!password.startsWith(PREFIX)) {
            return false;
        }
        String[] parts = password.split("\\$");
        return parts.length == 4;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) throws Exception {
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        return skf.generateSecret(spec).getEncoded();
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
