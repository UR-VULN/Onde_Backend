package com.onde.core.security;

import com.onde.core.entity.auth.RefreshToken;
import com.onde.core.entity.member.Member;
import com.onde.core.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionRevocationServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuthTokenBlacklistService authTokenBlacklistService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AuthSessionRevocationService authSessionRevocationService;

    @Test
    void revokeAllSessionsBlacklistsActiveAccessAndDeletesRefreshRecord() {
        Member member = Member.builder()
                .id(1L)
                .email("admin@onde.com")
                .authSubjectId("subject-uuid")
                .build();
        RefreshToken session = new RefreshToken(
                "subject-uuid",
                "refresh-token",
                "127.0.0.1",
                "ua-hash",
                "active-jti",
                3600L);

        when(refreshTokenRepository.findById("subject-uuid")).thenReturn(Optional.of(session));
        when(refreshTokenRepository.findById("admin@onde.com")).thenReturn(Optional.empty());
        when(jwtTokenProvider.getAccessTokenValidTimeInSeconds()).thenReturn(1800L);

        authSessionRevocationService.revokeAllSessions(member);

        verify(authTokenBlacklistService).blacklistJti("active-jti", 1800L);
        verify(refreshTokenRepository).deleteById("subject-uuid");
        verify(redisTemplate).delete("RT:admin@onde.com");
    }
}
