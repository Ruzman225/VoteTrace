package com.votetrace.lite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hashesPasswordsAndChecksTheCandidate() {
        char[] password = "correct horse battery".toCharArray();
        String hash = PasswordHasher.hash(password);

        assertNotEquals(new String(password), hash);
        assertTrue(PasswordHasher.matches(password, hash));
        assertFalse(PasswordHasher.matches("incorrect password".toCharArray(), hash));
    }

    @Test
    void rejectsPasswordsThatExceedBcryptUtf8Limit() {
        char[] password = "\u00e9".repeat(37).toCharArray();

        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(password));
    }
}
