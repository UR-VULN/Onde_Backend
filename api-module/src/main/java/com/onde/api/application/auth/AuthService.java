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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 5L;
    private static final String LOGIN_FAIL_PREFIX = "LOGIN_FAIL:admin:";

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 비밀번호 확인 일치 여부 검증
        if (request.getPasswordConfirm() != null && !request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        // 이메일 중복 검증
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 닉네임 중복 검증
        if (request.getNickname() != null && memberRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 비밀번호 암호화 및 Member 엔티티 생성
        MemberRole role = request.getRole() != null ? request.getRole() : MemberRole.USER;
        MemberStatus initialStatus = role == MemberRole.SELLER ? MemberStatus.PENDING : MemberStatus.ACTIVE;
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
    public boolean checkNicknameDuplicate(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return memberRepository.existsByEmail(email);
    }


    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }

        // 일반 로그인 시 관리자 권한 로그인 제한 (보안을 위해 동일하게 UNAUTHORIZED 예외 발생)
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

        // 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(member.getEmail(), member.getRole().getSecurityRole());
        String refreshTokenString = jwtTokenProvider.createRefreshToken(member.getEmail());

        // Refresh Token Redis 저장 (동일 이메일로 로그인 시 기존 토큰 덮어쓰기됨)
        RefreshToken refreshToken = new RefreshToken(
                member.getEmail(),
                refreshTokenString,
                jwtTokenProvider.getRefreshTokenValidTimeInSeconds()
        );
        refreshTokenRepository.save(refreshToken);

        // LoginResponse 객체 생성하여 반환
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(1800L) // 30분
                .memberId(member.getId())
                .role(member.getRole().name())
                .build();
    }

    @Transactional
    public LoginResponse adminLogin(LoginRequest request) {
        String failKey = LOGIN_FAIL_PREFIX + request.getEmail();
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= MAX_LOGIN_ATTEMPTS) {
            throw new BusinessException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        }

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    recordAdminLoginFailure(failKey);
                    return new UnauthorizedException(ErrorCode.UNAUTHORIZED);
                });

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            recordAdminLoginFailure(failKey);
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }

        // 관리자 권한이 아닌 사용자가 로그인 시도 시 실패 처리
        if (member.getRole() != MemberRole.USER_ADMIN && 
            member.getRole() != MemberRole.SELLER_ADMIN && 
            member.getRole() != MemberRole.SUPER_ADMIN) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }

        if (member.getStatus() == MemberStatus.BANNED) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        // 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(member.getEmail(), member.getRole().getSecurityRole());
        String refreshTokenString = jwtTokenProvider.createRefreshToken(member.getEmail());

        // Refresh Token Redis 저장
        RefreshToken refreshToken = new RefreshToken(
                member.getEmail(),
                refreshTokenString,
                jwtTokenProvider.getRefreshTokenValidTimeInSeconds()
        );
        refreshTokenRepository.save(refreshToken);

        // 로그인 성공 시 실패 카운터 초기화
        redisTemplate.delete(LOGIN_FAIL_PREFIX + member.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(600L) // 10분
                .memberId(member.getId())
                .role(member.getRole().name())
                .build();
    }

    private void recordAdminLoginFailure(String failKey) {
        redisTemplate.opsForValue().increment(failKey);
        redisTemplate.expire(failKey, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
    }

    @Transactional(readOnly = true)
    public TokenRefreshResponse refresh(String refreshToken) {
        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        // Token에서 식별자 추출
        String identifier = jwtTokenProvider.getSubject(refreshToken);

        // Redis에 저장된 토큰과 일치하는지 확인
        RefreshToken savedToken = refreshTokenRepository.findById(identifier)
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보가 없거나 만료되었습니다."));

        if (!savedToken.getRefreshToken().equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }

        // 회원 정보 조회 및 새로운 Access Token 발급
        Member member = memberRepository.findByEmail(identifier)
                .or(() -> memberRepository.findByProviderId(identifier))
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.createAccessToken(identifier, member.getRole().getSecurityRole());

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(1800L) // 30분
                .build();
    }
}
