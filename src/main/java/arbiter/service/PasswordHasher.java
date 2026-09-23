package arbiter.service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hashes and verifies passwords for account creation and replacement. */
final class PasswordHasher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 600_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    private PasswordHasher() {
    }

    static StoredPassword hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return new StoredPassword(Base64.getEncoder().encodeToString(derive(password, salt)),
                Base64.getEncoder().encodeToString(salt));
    }

    static boolean verify(String password, String hash, String salt) {
        try {
            byte[] expected = Base64.getDecoder().decode(hash);
            byte[] actual = derive(password, Base64.getDecoder().decode(salt));
            return MessageDigest.isEqual(actual, expected);
        } catch (IllegalArgumentException e) {
            throw new AuthException("Stored credentials are invalid", e);
        }
    }

    private static byte[] derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    record StoredPassword(String hash, String salt) {
    }
}
