package arbiter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

/** Checks the small password-storage helper independently of the JSON store. */
class PasswordHasherTest {
    @Test
    void hash_samePassword_usesIndependentSixteenByteSalts() {
        PasswordHasher.StoredPassword first = PasswordHasher.hash("password8");
        PasswordHasher.StoredPassword second = PasswordHasher.hash("password8");

        assertNotEquals(first.salt(), second.salt());
        assertNotEquals(first.hash(), second.hash());
        assertEquals(16, Base64.getDecoder().decode(first.salt()).length);
        assertEquals(32, Base64.getDecoder().decode(first.hash()).length);
    }

    @Test
    void verify_correctAndWrongPasswords_matchOnlyCorrectPassword() {
        PasswordHasher.StoredPassword stored = PasswordHasher.hash("password8");

        assertTrue(PasswordHasher.verify("password8", stored.hash(), stored.salt()));
        assertFalse(PasswordHasher.verify("wrongpass", stored.hash(), stored.salt()));
    }
}
