/*
 * Copyright 2015 - 2026 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.helper;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public final class Hashing {

    private static final String LEGACY_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int LEGACY_ITERATIONS = 1000;

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 200000;

    private static final int SALT_SIZE = 24;
    private static final int HASH_SIZE = 24;

    private static final SecretKeyFactory LEGACY_FACTORY;
    private static final SecretKeyFactory FACTORY;
    static {
        try {
            LEGACY_FACTORY = SecretKeyFactory.getInstance(LEGACY_ALGORITHM);
            FACTORY = SecretKeyFactory.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static class HashingResult {

        private final String hash;
        private final String salt;

        public HashingResult(String hash, String salt) {
            this.hash = hash;
            this.salt = salt;
        }

        public String getHash() {
            return hash;
        }

        public String getSalt() {
            return salt;
        }
    }

    private Hashing() {}

    private static byte[] function(SecretKeyFactory factory, char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_SIZE * Byte.SIZE);
            return factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            throw new SecurityException(e);
        }
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    public static HashingResult createHash(String password) {
        byte[] salt = new byte[SALT_SIZE];
        RANDOM.nextBytes(salt);
        byte[] hash = function(FACTORY, password.toCharArray(), salt, ITERATIONS);
        String hashedPassword = ALGORITHM + "$" + ITERATIONS + "$" + DataConverter.printHex(hash);
        return new HashingResult(hashedPassword, DataConverter.printHex(salt));
    }

    public static boolean validatePassword(String password, String hashedPasswordField, String saltHex) {
        byte[] salt = DataConverter.parseHex(saltHex);

        SecretKeyFactory factory;
        int iterations;
        String hashHex;
        String[] parts = hashedPasswordField.split("\\$");
        if (parts.length > 1) {
            factory = FACTORY;
            iterations = Integer.parseInt(parts[1]);
            hashHex = parts[2];
        } else {
            factory = LEGACY_FACTORY;
            iterations = LEGACY_ITERATIONS;
            hashHex = parts[0];
        }

        byte[] hash = DataConverter.parseHex(hashHex);
        return slowEquals(hash, function(factory, password.toCharArray(), salt, iterations));
    }

    /**
     * Compares two byte arrays in length-constant time. This comparison method
     * is used so that password hashes cannot be extracted from an on-line
     * system using a timing attack and then attacked off-line.
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

}
