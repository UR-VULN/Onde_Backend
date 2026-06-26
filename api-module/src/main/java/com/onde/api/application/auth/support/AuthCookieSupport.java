package com.onde.api.application.auth.support;

import com.onde.core.config.AuthCookieProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieSupport {

    private static final long ACCESS_MAX_AGE_SECONDS = 30 * 60L;
    private static final long REFRESH_MAX_AGE_SECONDS = 14 * 24 * 60 * 60L;

    private final AuthCookieProperties authCookieProperties;

    public ResponseCookie accessTokenCookie(String accessToken) {
        return baseCookie("accessToken", accessToken)
                .maxAge(ACCESS_MAX_AGE_SECONDS)
                .build();
    }

    public ResponseCookie refreshTokenCookie(String refreshToken) {
        return baseCookie("refreshToken", refreshToken)
                .maxAge(REFRESH_MAX_AGE_SECONDS)
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return baseCookie("accessToken", "")
                .maxAge(0)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return baseCookie("refreshToken", "")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(authCookieProperties.isSecure())
                .path("/")
                .sameSite(authCookieProperties.getSameSite());
    }
}
