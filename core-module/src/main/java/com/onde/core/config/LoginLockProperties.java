package com.onde.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로그인 실패 잠금 정책 (가이드라인 3-2).
 */
@ConfigurationProperties(prefix = "onde.security.login")
public class LoginLockProperties {

    private int maxFailedAttempts = 5;
    private int lockMinutes = 30;

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public int getLockMinutes() {
        return lockMinutes;
    }

    public void setLockMinutes(int lockMinutes) {
        this.lockMinutes = lockMinutes;
    }
}
