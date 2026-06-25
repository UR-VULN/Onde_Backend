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
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;
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
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 비밀번호 확인 일치 여부 검증
        if (request.getPasswordConfirm() != null && !request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        // [보안 패치] 패스워드 강력 검증 (KISA 가이드라인: 반복, 연속, 개인정보 포함 차단)
        validatePasswordPolicy(request);

        // 이메일 또는 닉네임 중복 검증 (에러 메시지 일반화)
        boolean isEmailDuplicate = memberRepository.existsByEmail(request.getEmail());
        boolean isNicknameDuplicate = memberRepository.existsByNickname(request.getNickname());
        
        if (isEmailDuplicate || isNicknameDuplicate) {
            // 원인과 무관하게 단일 메시지로 통일하여 계정 존재 여부 노출 차단
            throw new IllegalArgumentException("입력하신 값이 유효하지 않습니다.");
        }

        // 비밀번호 암호화 및 Member 엔티티 생성
        MemberRole role = request.getRole() != null ? request.getRole() : MemberRole.USER;
        
        // 관리자 권한 가입 차단 (Role Injection 방어)
        if (role == MemberRole.SUPER_ADMIN || role == MemberRole.USER_ADMIN || role == MemberRole.SELLER_ADMIN) {
            throw new IllegalArgumentException("허용되지 않은 가입 권한입니다.");
        }
        
        // XSS 방어를 위한 HTML 특수문자 이스케이프 처리
        String safeName = escapeHtml(request.getName());
        String safePhoneNumber = escapeHtml(request.getPhoneNumber());
        String safeNickname = escapeHtml(request.getNickname());

        MemberStatus initialStatus = role == MemberRole.SELLER ? MemberStatus.PENDING : MemberStatus.ACTIVE;
        Member member = Member.builder()
                .email(request.getEmail())
                .name(safeName)
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(safePhoneNumber)
                .nickname(safeNickname)
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
        String redisKey = "login_attempts:" + request.getEmail();
        String attemptsStr = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (attemptsStr != null && Integer.parseInt(attemptsStr) >= 5) {
            throw new IllegalArgumentException("인증 실패 5회 초과로 30분간 계정이 잠깁니다.");
        }

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            stringRedisTemplate.opsForValue().increment(redisKey);
            if (attemptsStr == null) {
                stringRedisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);
            }
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }
        
        // 로그인 성공 시 실패 횟수 초기화
        stringRedisTemplate.delete(redisKey);

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
        String redisKey = "login_attempts:admin:" + request.getEmail();
        String attemptsStr = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (attemptsStr != null && Integer.parseInt(attemptsStr) >= 5) {
            throw new IllegalArgumentException("관리자 인증 실패 5회 초과로 30분간 계정이 잠깁니다.");
        }

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            stringRedisTemplate.opsForValue().increment(redisKey);
            if (attemptsStr == null) {
                stringRedisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);
            }
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }
        
        // 로그인 성공 시 실패 횟수 초기화
        stringRedisTemplate.delete(redisKey);

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

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(1800L) // 30분
                .memberId(member.getId())
                .role(member.getRole().name())
                .build();
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

    private String escapeHtml(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    private void validatePasswordPolicy(SignupRequest request) {
        String password = request.getPassword();
        if (password == null) return;

        // 1. 3자리 이상 동일한 문자/숫자 반복 체크 (예: aaa, 111)
        if (password.matches(".*(.)\\1{2,}.*")) {
            throw new IllegalArgumentException("비밀번호에 3자리 이상 동일한 문자나 숫자를 반복해서 사용할 수 없습니다.");
        }

        // 2. 키보드 및 알파벳/숫자 연속 문자 3자리 이상 체크
        String[] sequentialPatterns = {
            "01234567890", "09876543210", 
            "abcdefghijklmnopqrstuvwxyz", "zyxwvutsrqponmlkjihgfedcba",
            "qwertyuiop", "poiuytrewq",
            "asdfghjkl", "lkjhgfdsa",
            "zxcvbnm", "mnbvcxz"
        };
        String lowerPassword = password.toLowerCase();
        for (String pattern : sequentialPatterns) {
            for (int i = 0; i < pattern.length() - 2; i++) {
                String sub = pattern.substring(i, i + 3);
                if (lowerPassword.contains(sub)) {
                    throw new IllegalArgumentException("비밀번호에 3자리 이상 연속된 문자나 숫자를 사용할 수 없습니다.");
                }
            }
        }

        // 3. 개인정보 포함 체크 (이메일 아이디, 이름, 전화번호)
        if (request.getEmail() != null) {
            String emailId = request.getEmail().split("@")[0];
            if (emailId.length() >= 3 && lowerPassword.contains(emailId.toLowerCase())) {
                throw new IllegalArgumentException("비밀번호에 이메일(아이디)을 포함할 수 없습니다.");
            }
        }
        if (request.getName() != null && request.getName().length() >= 2) {
            if (lowerPassword.contains(request.getName().toLowerCase())) {
                throw new IllegalArgumentException("비밀번호에 이름을 포함할 수 없습니다.");
            }
        }
        if (request.getPhoneNumber() != null) {
            String cleanPhone = request.getPhoneNumber().replace("-", "");
            if (cleanPhone.length() >= 4 && lowerPassword.contains(cleanPhone)) {
                throw new IllegalArgumentException("비밀번호에 전화번호를 포함할 수 없습니다.");
            }
        }
    }

    @Transactional
    public void logout(String email, String accessToken) {
        // 1. Refresh Token Redis 저장소에서 토큰 삭제
        refreshTokenRepository.deleteById(email);

        // 2. Access Token 유효시간 계산 후 Redis 블랙리스트 등록
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            java.util.Date expiration = jwtTokenProvider.getExpirationDate(accessToken);
            long remainTime = expiration.getTime() - System.currentTimeMillis();
            if (remainTime > 0) {
                stringRedisTemplate.opsForValue().set(
                        "BL:" + accessToken,
                        "logout",
                        remainTime,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                );
            }
        }
    }
}

