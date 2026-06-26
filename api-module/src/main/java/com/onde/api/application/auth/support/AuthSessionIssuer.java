package com.onde.api.application.auth.support;

import com.onde.api.application.auth.dto.AuthSessionResult;
import com.onde.api.application.auth.dto.LoginResponse;
import com.onde.core.entity.auth.RefreshToken;
import com.onde.core.entity.member.Member;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.RefreshTokenRepository;
import com.onde.core.security.AuthTokenBlacklistService;
import com.onde.core.security.JwtTokenProvider;
import com.onde.core.security.PasswordLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthSessionIssuer {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenBlacklistService authTokenBlacklistService;
    private final PasswordLifecycleService passwordLifecycleService;

    @Transactional
    public AuthSessionResult issue(Member member, HttpServletRequest request) {
        String subjectId = member.ensureAuthSubjectId();
        memberRepository.save(member);

        AuthClientContext clientContext = AuthClientContext.from(request);
        killExistingSession(subjectId);

        String accessToken = jwtTokenProvider.createAccessToken(subjectId, member.getRole().getSecurityRole());
        String accessJti = jwtTokenProvider.getJti(accessToken);
        String refreshTokenString = jwtTokenProvider.createRefreshToken(subjectId);

        RefreshToken refreshToken = new RefreshToken(
                subjectId,
                refreshTokenString,
                clientContext.getClientIp(),
                clientContext.getUserAgentHash(),
                accessJti,
                jwtTokenProvider.getRefreshTokenValidTimeInSeconds()
        );
        refreshTokenRepository.save(refreshToken);

        LoginResponse profile = LoginResponse.builder()
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidTimeInSeconds())
                .role(member.getRole().name())
                .passwordChangeRequired(passwordLifecycleService.isPasswordExpired(member))
                .build();

        return AuthSessionResult.builder()
                .profile(profile)
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .build();
    }

    private void killExistingSession(String subjectId) {
        Optional<RefreshToken> existing = refreshTokenRepository.findById(subjectId);
        if (existing.isEmpty()) {
            return;
        }
        String oldJti = existing.get().getActiveAccessJti();
        if (StringUtils.hasText(oldJti)) {
            authTokenBlacklistService.blacklistJti(oldJti, jwtTokenProvider.getAccessTokenValidTimeInSeconds());
        }
        refreshTokenRepository.deleteById(subjectId);
    }
}
