package com.onde.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

/**
 * [보안 강화 - WEB-9 / ADMIN-9] 로그아웃 후 토큰 재사용을 방지하기 위한 JWT 블랙리스트 서비스
 * Redis를 활용하여 로그아웃된 토큰을 남은 만료시간 동안 적재하고 차단합니다.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String BLACKLIST_PREFIX = "token_blacklist:";

    /**
     * 토큰을 블랙리스트에 등록합니다.
     * @param token JWT 토큰
     * @param remainingTimeInSeconds 토큰의 남은 유효 시간 (초)
     */
    public void blacklistToken(String token, long remainingTimeInSeconds) {
        if (remainingTimeInSeconds > 0) {
            stringRedisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token,
                    "logout",
                    remainingTimeInSeconds,
                    TimeUnit.SECONDS
            );
        }
    }

    /**
     * 토큰이 블랙리스트에 등록되어 있는지 확인합니다.
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
