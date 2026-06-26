package com.onde.core.security;

import com.onde.core.util.AesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PassportFieldCodecTest {

    private PassportFieldCodec codec;

    @BeforeEach
    void setUp() {
        codec = new PassportFieldCodec(new AesUtil("0123456789abcdef0123456789abcdef"));
    }

    @Test
    void encryptsAndDecryptsPassport() {
        String plain = "M12345678";

        String stored = codec.encryptForStorage(plain);

        assertNotEquals(plain, stored);
        assertEquals(plain, codec.decryptForAuthorizedRead(stored));
    }

    @Test
    void masksDecryptedPassport() {
        String stored = codec.encryptForStorage("M12345678");

        assertEquals("M1****78", codec.maskForDisplay(stored));
    }

    @Test
    void decryptFallsBackToLegacyPlaintext() {
        assertEquals("M12345678", codec.decryptForAuthorizedRead("M12345678"));
    }
}
