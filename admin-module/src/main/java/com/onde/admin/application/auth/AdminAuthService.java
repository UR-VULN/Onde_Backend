package com.onde.admin.application.auth;

import com.onde.core.entity.member.Member;
import com.onde.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final com.onde.core.security.JwtTokenProvider jwtTokenProvider;
    private final com.onde.core.repository.RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void changePassword(Long adminId, String rawPassword, String newRawPassword) {
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(rawPassword, admin.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        admin.updatePassword(passwordEncoder.encode(newRawPassword));
        
        // 보안을 위해 기존 Refresh Token 삭제
        String redisKey = "auth:refresh:" + admin.getId();
        redisTemplate.delete(redisKey);
    }

    @Transactional
    public com.onde.admin.application.auth.dto.AdminLoginResponse login(com.onde.admin.application.auth.dto.AdminLoginRequest request, String clientIp) {
        // IP 기반 Rate Limiting (1분당 10회 제한)
        String ipKey = "rate_limit:ip:admin:" + clientIp;
        Long reqCount = redisTemplate.opsForValue().increment(ipKey);
        if (reqCount != null && reqCount == 1) {
            redisTemplate.expire(ipKey, 60, java.util.concurrent.TimeUnit.SECONDS);
        }
        if (reqCount != null && reqCount > 10) {
            throw new com.onde.core.exception.BusinessException(com.onde.core.exception.ErrorCode.TOO_MANY_REQUESTS);
        }

        // 계정 잠금 여부 확인
        String lockKey = "lockout:admin:" + request.getEmail();
        String failedCountKey = "failed_login:admin:" + request.getEmail();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new com.onde.core.exception.UnauthorizedException(com.onde.core.exception.ErrorCode.ACCOUNT_LOCKED);
        }

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new com.onde.core.exception.UnauthorizedException(com.onde.core.exception.ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            Long fails = redisTemplate.opsForValue().increment(failedCountKey);
            if (fails != null && fails == 1) {
                redisTemplate.expire(failedCountKey, 30, java.util.concurrent.TimeUnit.MINUTES);
            }
            if (fails != null && fails >= 5) {
                redisTemplate.opsForValue().set(lockKey, "locked", 30, java.util.concurrent.TimeUnit.MINUTES);
                redisTemplate.delete(failedCountKey);
                throw new com.onde.core.exception.UnauthorizedException(com.onde.core.exception.ErrorCode.ACCOUNT_LOCKED);
            }
            throw new com.onde.core.exception.UnauthorizedException(com.onde.core.exception.ErrorCode.UNAUTHORIZED);
        }

        redisTemplate.delete(failedCountKey);

        if (member.getRole() != com.onde.core.entity.member.MemberRole.USER_ADMIN && 
            member.getRole() != com.onde.core.entity.member.MemberRole.SELLER_ADMIN && 
            member.getRole() != com.onde.core.entity.member.MemberRole.SUPER_ADMIN) {
            throw new com.onde.core.exception.UnauthorizedException(com.onde.core.exception.ErrorCode.UNAUTHORIZED);
        }

        if (member.getStatus() == com.onde.core.entity.member.MemberStatus.BANNED) {
            throw new com.onde.core.exception.ForbiddenException(com.onde.core.exception.ErrorCode.FORBIDDEN);
        }

        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(member.getId()));
        String refreshTokenString = jwtTokenProvider.createRefreshToken(String.valueOf(member.getId()));

        com.onde.core.entity.auth.RefreshToken refreshToken = new com.onde.core.entity.auth.RefreshToken(
                String.valueOf(member.getId()),
                refreshTokenString,
                jwtTokenProvider.getRefreshTokenValidTimeInSeconds()
        );
        refreshTokenRepository.save(refreshToken);

        // [단일 세션 정책] Access Token을 Redis에 저장
        redisTemplate.opsForValue().set(
                "active_access_token:" + member.getId(),
                accessToken,
                900L,
                java.util.concurrent.TimeUnit.SECONDS
        );

        return com.onde.admin.application.auth.dto.AdminLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(900L) // 15분
                .memberId(member.getId())
                .role(member.getRole().name())
                .build();
    }
}