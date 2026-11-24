package com.votetrace.lite;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;

public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(char[] password) {
        requireBcryptLength(password);
        return BCrypt.hashpw(new String(password), BCrypt.gensalt(12));
    }

    public static boolean matches(char[] password, String passwordHash) {
        if (utf8Length(password) > 72) {
            return false;
        }
        return BCrypt.checkpw(new String(password), passwordHash);
    }

    static int utf8Length(char[] password) {
        return new String(password).getBytes(StandardCharsets.UTF_8).length;
    }

    private static void requireBcryptLength(char[] password) {
        if (utf8Length(password) > 72) {
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes.");
        }
    }
}
