package com.onde.api.application.auth.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/** 토큰 발급·검증 시 클라이언트 환경 바인딩 */
public final class AuthClientContext {

    private final String clientIp;
    private final String userAgentHash;

    private AuthClientContext(String clientIp, String userAgentHash) {
        this.clientIp = clientIp;
        this.userAgentHash = userAgentHash;
    }

    public static AuthClientContext from(HttpServletRequest request) {
        if (request == null) {
            return new AuthClientContext("", "");
        }
        String ip = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String hash = hashUserAgent(userAgent);
        return new AuthClientContext(ip, hash);
    }

    public static String hashUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "";
        }
        return DigestUtils.md5DigestAsHex(userAgent.getBytes(StandardCharsets.UTF_8));
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
    }

    public boolean matches(com.onde.core.entity.auth.RefreshToken session) {
        if (session == null) {
            return true;
        }
        if (!StringUtils.hasText(session.getClientIp())) {
            return true;
        }
        return clientIp.equals(session.getClientIp())
                && userAgentHash.equals(session.getClientUserAgentHash());
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getUserAgentHash() {
        return userAgentHash;
    }
}
