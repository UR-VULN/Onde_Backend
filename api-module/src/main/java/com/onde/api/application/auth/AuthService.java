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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public LoginResponse login(LoginRequest request) {
        // 1. 이메일이 없을 때 던지는 에러를 400(IllegalArgumentException)과 모호한 메시지로 변경
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));

        if (member.isAccountLocked()) {
            throw new IllegalArgumentException("비밀번호 5회 오류로 계정이 잠겼습니다. 관리자에게 문의하세요.");
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            // 실패 카운트는 내부적으로 계속 올리되,
            member.addLoginFailCount();
            memberRepository.save(member);
            
            // 2. 해커에게 보여주는 메시지는 이메일이 없을 때와 완벽하게 동일하게 뭉뚱그림
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        member.resetLoginFailCount();
        memberRepository.save(member);

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

        String accessToken = jwtTokenProvider.createAccessToken(member.getEmail(), member.getRole().getSecurityRole());
        String refreshTokenString = jwtTokenProvider.createRefreshToken(member.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID().toString()) // 무작위 UUID
                .email(member.getEmail())         // 이메일은 값으로 보관
                .refreshToken(refreshTokenString)
                .expiration(jwtTokenProvider.getRefreshTokenValidTimeInSeconds())
                .build();
        
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(1800L)
                .memberId(member.getId())
                .role(member.getRole().name())
                .build();
    }

    // 어드민 로그인
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public LoginResponse adminLogin(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다."));

        if (member.isAccountLocked()) {
            throw new IllegalArgumentException("비밀번호 5회 오류로 계정이 잠겼습니다. 관리자에게 문의하세요.");
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            member.addLoginFailCount();
            memberRepository.save(member);
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        member.resetLoginFailCount();
        memberRepository.save(member);

        if (member.getRole() != MemberRole.USER_ADMIN && 
            member.getRole() != MemberRole.SELLER_ADMIN && 
            member.getRole() != MemberRole.SUPER_ADMIN) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }

        if (member.getStatus() == MemberStatus.BANNED) {
            throw new ForbiddenException(ErrorCode.FORBIDDEN);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getEmail(), member.getRole().getSecurityRole());
        String refreshTokenString = jwtTokenProvider.createRefreshToken(member.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
            .id(UUID.randomUUID().toString()) // 새로운 Key가 될 UUID
            .email(member.getEmail())         // 이메일은 값으로 저장
            .refreshToken(refreshTokenString)
            .expiration(jwtTokenProvider.getRefreshTokenValidTimeInSeconds())
            .build();

        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(1800L)
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

        String email = jwtTokenProvider.getSubject(refreshToken);

        // Redis에 저장된 토큰과 일치하는지 확인
        RefreshToken savedToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보가 없거나 만료되었습니다."));

        if (!savedToken.getRefreshToken().equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }

        // 회원 정보 조회 및 새로운 Access Token 발급
        Member member = memberRepository.findByEmail(email)
                .or(() -> memberRepository.findByProviderId(email))
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        String newAccessToken = jwtTokenProvider.createAccessToken(email, member.getRole().getSecurityRole());

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(1800L) // 30분
                .build();
    }
}
