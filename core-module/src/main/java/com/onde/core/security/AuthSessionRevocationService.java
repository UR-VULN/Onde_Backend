package com.onde.core.security;

import com.onde.core.entity.auth.RefreshToken;
import com.onde.core.entity.member.Member;
import com.onde.core.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 로그아웃·비밀번호 변경·블랙리스트 등에서 Refresh 세션 삭제 및 Access Token 즉시 무효화.
 */
@Service
@RequiredArgsConstructor
public class AuthSessionRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthTokenBlacklistService authTokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    public void revokeAllSessions(Member member) {
        if (member == null) {
            return;
        }
        String authSubjectId = member.getAuthSubjectId();
        if (StringUtils.hasText(authSubjectId)) {
            revokeBySubjectId(Objects.requireNonNull(authSubjectId));
        }
        String email = member.getEmail();
        if (StringUtils.hasText(email)) {
            revokeBySubjectId(Objects.requireNonNull(email));
            redisTemplate.delete(legacyRefreshKey(Objects.requireNonNull(email)));
        }
    }

    private void revokeBySubjectId(@NonNull String subjectId) {
        refreshTokenRepository.findById(subjectId).ifPresent(this::revokeSession);
    }

    private void revokeSession(RefreshToken session) {
        if (StringUtils.hasText(session.getActiveAccessJti())) {
            authTokenBlacklistService.blacklistJti(
                    session.getActiveAccessJti(),
                    jwtTokenProvider.getAccessTokenValidTimeInSeconds());
        }
        refreshTokenRepository.deleteById(Objects.requireNonNull(session.getSubjectId()));
    }

    @NonNull
    private static String legacyRefreshKey(@NonNull String email) {
        return "RT:" + email;
    }
}
