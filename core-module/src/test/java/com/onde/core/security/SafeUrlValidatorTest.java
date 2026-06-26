package com.onde.core.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeUrlValidatorTest {

    @Test
    void allowsWhitelistedHttpsHost() {
        assertDoesNotThrow(() -> SafeUrlValidator.assertAllowedHttpUrl(
                "https://api.odcloud.kr/api/nts-businessman/v1/validate",
                Set.of("api.odcloud.kr")
        ));
    }

    @Test
    void rejectsFileScheme() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                SafeUrlValidator.assertAllowedHttpUrl("file:///etc/passwd", Set.of("api.odcloud.kr")));
        assertTrue(ex.getMessage().contains("http"));
    }

    @Test
    void allowsExplicitLoopbackWhenAllowlisted() {
        assertDoesNotThrow(() -> SafeUrlValidator.assertAllowedHttpUrl(
                "http://127.0.0.1/admin",
                Set.of("127.0.0.1")
        ));
    }

    @Test
    void rejectsPrivateLoopbackHost() {
        assertThrows(IllegalArgumentException.class, () ->
                SafeUrlValidator.assertAllowedHttpUrl("http://127.0.0.1/admin", Set.of("api.odcloud.kr")));
    }

    @Test
    void rejectsMetadataPrivateHostEvenWhenAllowlisted() {
        assertThrows(IllegalArgumentException.class, () ->
                SafeUrlValidator.assertAllowedHttpUrl("http://169.254.169.254/", Set.of("169.254.169.254")));
    }

    @Test
    void rejectsHostOutsideAllowlist() {
        assertThrows(IllegalArgumentException.class, () ->
                SafeUrlValidator.assertAllowedHttpUrl("https://evil.example.com", Set.of("api.odcloud.kr")));
    }

    @Test
    void parsesAllowedHostsFromConfig() {
        Set<String> hosts = SafeUrlValidator.parseAllowedHosts(" api.odcloud.kr , onde.click ");
        assertTrue(hosts.contains("api.odcloud.kr"));
        assertTrue(hosts.contains("onde.click"));
    }
}
