package com.onde.api.application.auth;

import com.onde.api.application.auth.dto.*;
import com.onde.core.entity.auth.RefreshToken;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.member.MemberStatus;
import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ForbiddenException;
import com.onde.core.exception.UnauthorizedException;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.RefreshTokenRepository;
import com.onde.core.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (request.getPasswordConfirm() != null && !request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (request.getNickname() != null && memberRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        MemberRole role = MemberRole.USER;
        MemberStatus initialStatus = MemberStatus.ACTIVE;
        Member member = Member.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .nickname(request.getNickname())
                .age(request.getAge())
                .role(role)
                .status(initialStatus)
                .build();

        Member savedMember = memberRepository.save(member);

        return SignupResponse.builder()
                .memberId(savedMember.getId())
                .email(savedMember.getEmail())
                .name(savedMember.getName())
                .role(savedMember.getRole())
                .status(savedMember.getStatus())
                .nickname(savedMember.getNickname())
                .age(savedMember.getAge())
                .createdAt(savedMember.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public boolean checkNicknameDuplicate(String nickname, String clientIp) {
        String ipKey = "rate_limit:ip:enum:" + clientIp;
        Long reqCount = redisTemplate.opsForValue().increment(ipKey);
        if (reqCount != null && reqCount == 1) {
            redisTemplate.expire(ipKey, 60, java.util.concurrent.TimeUnit.SECONDS);
        }
        if (reqCount != null && reqCount > 10) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        return memberRepository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email, String clientIp) {
        String ipKey = "rate_limit:ip:enum:" + clientIp;
        Long reqCount = redisTemplate.opsForValue().increment(ipKey);
        if (reqCount != null && reqCount == 1) {
            redisTemplate.expire(ipKey, 60, java.util.concurrent.TimeUnit.SECONDS);
        }
        if (reqCount != null && reqCount > 10) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        return memberRepository.existsByEmail(email);
    }


    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        String ipKey = "rate_limit:ip:" + clientIp;
        Long reqCount = redisTemplate.opsForValue().increment(ipKey);
        if (reqCount != null && reqCount == 1) {
            redisTemplate.expire(ipKey, 60, java.util.concurrent.TimeUnit.SECONDS);
        }
        if (reqCount != null && reqCount > 10) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        String lockKey = "lockout:" + request.getEmail();
        String failedCountKey = "failed_login:" + request.getEmail();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new UnauthorizedException(ErrorCode.ACCOUNT_LOCKED);
        }

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            Long fails = redisTemplate.opsForValue().increment(failedCountKey);
            if (fails != null && fails == 1) {
                redisTemplate.expire(failedCountKey, 30, java.util.concurrent.TimeUnit.MINUTES);
            }
            if (fails != null && fails >= 5) {
                redisTemplate.opsForValue().set(lockKey, "locked", 30, java.util.concurrent.TimeUnit.MINUTES);
                redisTemplate.delete(failedCountKey);
                throw new UnauthorizedException(ErrorCode.ACCOUNT_LOCKED);
            }
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }

        redisTemplate.delete(failedCountKey);

        if (member.getRole() == MemberRole.USER_ADMIN || 
            member.getRole() == MemberRole.SELLER_ADMIN || 
            member.getRole() == MemberRole.SUPER_ADMIN) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }

        if (member.getRole() == MemberRole.BLACKLIST || member.getStatus() == MemberStatus.BANNED) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        if (member.getRole() == MemberRole.SELLER && member.getStatus() == MemberStatus.PENDING) {
            throw new BusinessException(ErrorCode.SELLER_PENDING_APPROVAL);
        }

        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(member.getId()));
        String refreshTokenString = jwtTokenProvider.createRefreshToken(String.valueOf(member.getId()));

        RefreshToken refreshToken = new RefreshToken(
                String.valueOf(member.getId()),
                refreshTokenString,
                jwtTokenProvider.getRefreshTokenValidTimeInSeconds()
        );
        refreshTokenRepository.save(refreshToken);

        // [단일 세션 정책] Access Token을 Redis에 저장 (15분 유지)하여 기존 발급된 토큰 무효화
        redisTemplate.opsForValue().set(
                "active_access_token:" + member.getId(),
                accessToken,
                900L,
                java.util.concurrent.TimeUnit.SECONDS
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(900L) // 15분
                .memberId(member.getId())
                .role(member.getRole().name())
                .build();
    }



    @Transactional
    public TokenRefreshResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        String identifier = jwtTokenProvider.getSubject(refreshToken);

        RefreshToken savedToken = refreshTokenRepository.findById(identifier)
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보가 없거나 만료되었습니다."));

        if (!savedToken.getRefreshToken().equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }

        Member member = memberRepository.findById(Long.parseLong(identifier))
                .orElseThrow(() -> new com.onde.core.exception.UnauthorizedException(com.onde.core.exception.ErrorCode.UNAUTHORIZED));

        String newAccessToken = jwtTokenProvider.createAccessToken(identifier);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(identifier);

        RefreshToken newRefreshTokenEntity = new RefreshToken(
                identifier,
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenValidTimeInSeconds()
        );
        refreshTokenRepository.save(newRefreshTokenEntity);

        // [단일 세션 정책] 새 Access Token을 Redis에 갱신
        redisTemplate.opsForValue().set(
                "active_access_token:" + identifier,
                newAccessToken,
                900L,
                java.util.concurrent.TimeUnit.SECONDS
        );

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900L) // 15분
                .build();
    }
}
