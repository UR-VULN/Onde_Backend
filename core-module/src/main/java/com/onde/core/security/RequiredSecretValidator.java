package com.onde.core.security;

import java.nio.charset.StandardCharsets;

/** 환경 변수 미설정 시 취약한 기본 키 대신 즉시 기동 실패(Fail-Fast) */
public final class RequiredSecretValidator {

    private static final int JWT_SECRET_MIN_BYTES = 32;
    private static final int AES_SECRET_KEY_BYTES = 32;

    private RequiredSecretValidator() {
    }

    public static String requireJwtSecret(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret is not configured. Set environment variable JWT_SECRET before starting the application.");
        }
        if (secretKey.getBytes(StandardCharsets.UTF_8).length < JWT_SECRET_MIN_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 bytes for HS256. Configure a stronger JWT_SECRET.");
        }
        return secretKey;
    }

    public static String requireAesSecretKey(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "encryption.aes.secret-key is not configured. Set environment variable AES_SECRET_KEY before starting the application.");
        }
        if (secretKey.getBytes(StandardCharsets.UTF_8).length != AES_SECRET_KEY_BYTES) {
            throw new IllegalStateException(
                    "encryption.aes.secret-key must be exactly 32 bytes for AES-256. Configure AES_SECRET_KEY accordingly.");
        }
        return secretKey;
    }
}
