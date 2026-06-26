package com.onde.core.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequiredSecretValidatorTest {

    private static final String VALID_JWT_SECRET = "01234567890123456789012345678901";
    private static final String VALID_AES_SECRET = "01234567890123456789012345678901";

    @Test
    void acceptsValidJwtSecret() {
        assertEquals(VALID_JWT_SECRET, RequiredSecretValidator.requireJwtSecret(VALID_JWT_SECRET));
    }

    @Test
    void rejectsMissingJwtSecret() {
        assertThrows(IllegalStateException.class, () -> RequiredSecretValidator.requireJwtSecret(null));
        assertThrows(IllegalStateException.class, () -> RequiredSecretValidator.requireJwtSecret("   "));
    }

    @Test
    void rejectsShortJwtSecret() {
        assertThrows(IllegalStateException.class, () -> RequiredSecretValidator.requireJwtSecret("too-short"));
    }

    @Test
    void acceptsValidAesSecret() {
        assertEquals(VALID_AES_SECRET, RequiredSecretValidator.requireAesSecretKey(VALID_AES_SECRET));
    }

    @Test
    void rejectsMissingAesSecret() {
        assertThrows(IllegalStateException.class, () -> RequiredSecretValidator.requireAesSecretKey(null));
    }

    @Test
    void rejectsInvalidAesSecretLength() {
        assertThrows(IllegalStateException.class, () -> RequiredSecretValidator.requireAesSecretKey("short-key"));
        assertDoesNotThrow(() -> RequiredSecretValidator.requireAesSecretKey(VALID_AES_SECRET));
    }
}
