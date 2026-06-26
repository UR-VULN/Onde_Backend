package com.onde.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 로그아웃·중복 로그인 시 Access Token 즉시 무효화 (Redis TTL = 잔여 유효시간).
 */
@Service
@RequiredArgsConstructor
public class AuthTokenBlacklistService {

    private static final String JTI_PREFIX = "auth:blacklist:jti:";
    private static final String TOKEN_HASH_PREFIX = "auth:blacklist:token:";

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public void blacklistAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank() || !jwtTokenProvider.validateToken(accessToken)) {
            return;
        }
        long ttlSeconds = jwtTokenProvider.getRemainingValiditySeconds(accessToken);
        if (ttlSeconds <= 0) {
            return;
        }
        String jti = jwtTokenProvider.getJti(accessToken);
        if (jti != null && !jti.isBlank()) {
            redisTemplate.opsForValue().set(JTI_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
            return;
        }
        String tokenHash = md5Hex(accessToken);
        redisTemplate.opsForValue().set(TOKEN_HASH_PREFIX + tokenHash, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    public void blacklistJti(String jti, long ttlSeconds) {
        if (jti == null || jti.isBlank() || ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(JTI_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean isBlacklisted(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        String jti = jwtTokenProvider.getJti(accessToken);
        if (jti != null && !jti.isBlank() && Boolean.TRUE.equals(redisTemplate.hasKey(JTI_PREFIX + jti))) {
            return true;
        }
        String tokenHash = md5Hex(accessToken);
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_HASH_PREFIX + tokenHash));
    }

    private static String md5Hex(@NonNull String value) {
        byte[] bytes = Objects.requireNonNull(value.getBytes(StandardCharsets.UTF_8));
        return DigestUtils.md5DigestAsHex(bytes);
    }
}
