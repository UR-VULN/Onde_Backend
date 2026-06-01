package com.onde.api.application.auth;

import com.onde.api.application.auth.dto.*;
import com.onde.core.entity.auth.RefreshToken;
import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberStatus;
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

    @Transactional
    public String signup(SignupRequest request) {
        // 이메일 중복 검증
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 암호화 및 Member 엔티티 생성
        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .status(MemberStatus.ACTIVE)
                .build();

        memberRepository.save(member);

        return "회원가입이 성공적으로 완료되었습니다.";
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
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

        // API 명세서 규격에 맞게 LoginResponse 객체 생성하여 반환
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
        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        // 2. Token에서 식별자(email 혹은 providerId) 추출
        String identifier = jwtTokenProvider.getSubject(refreshToken);

        // 3. Redis에 저장된 토큰과 일치하는지 확인
        RefreshToken savedToken = refreshTokenRepository.findById(identifier)
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보가 없거나 만료되었습니다."));

        if (!savedToken.getRefreshToken().equals(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }

        // 4. 회원 정보 조회 및 새로운 Access Token 발급
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
