package com.onde.admin.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AdminTokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "BL:admin:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String token, long remainingSeconds) {
        if (remainingSeconds > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", remainingSeconds, TimeUnit.SECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
