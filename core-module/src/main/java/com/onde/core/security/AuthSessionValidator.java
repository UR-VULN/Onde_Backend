package com.onde.core.security;

import com.onde.core.entity.auth.RefreshToken;
import com.onde.core.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthSessionValidator {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenBlacklistService authTokenBlacklistService;
    private final RefreshTokenRepository refreshTokenRepository;

    public boolean isAccessTokenAllowed(String accessToken, HttpServletRequest request) {
        if (!jwtTokenProvider.validateToken(accessToken)) {
            return false;
        }
        if (authTokenBlacklistService.isBlacklisted(accessToken)) {
            return false;
        }

        String subject = jwtTokenProvider.getSubject(accessToken);
        String jti = jwtTokenProvider.getJti(accessToken);
        if (!StringUtils.hasText(subject)) {
            return true;
        }
        Optional<RefreshToken> session = refreshTokenRepository.findById(Objects.requireNonNull(subject));

        if (session.isEmpty()) {
            // 레거시(이메일 subject) 또는 세션 레코드 만료 — 서명·블랙리스트만 검증
            return true;
        }

        RefreshToken activeSession = session.get();
        AuthClientContext clientContext = AuthClientContext.from(request);
        if (!clientContext.matches(activeSession)) {
            return false;
        }

        if (StringUtils.hasText(jti) && StringUtils.hasText(activeSession.getActiveAccessJti())) {
            return jti.equals(activeSession.getActiveAccessJti());
        }

        return true;
    }
}
