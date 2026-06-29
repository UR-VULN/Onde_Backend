package com.onde.api.application.auth;

import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.entity.member.MemberStatus;
import com.onde.core.entity.auth.RefreshToken;
import com.onde.core.repository.MemberRepository;
import com.onde.core.repository.RefreshTokenRepository;
import com.onde.core.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final StringRedisTemplate redisTemplate;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    
    private static final String REDIS_PREFIX = "EMAIL_VERIFY:";
    private static final long CODE_TTL = 3 * 60; // 3 minutes

    @Transactional
    public void sendVerificationCode(String email, Member guestMember) {
        sendVerificationCode(email);
    }

    @Transactional
    public void sendVerificationCode(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }

        // 6자리 난수 생성
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Redis 저장 (TTL 3분)
        redisTemplate.opsForValue().set(REDIS_PREFIX + email, code, CODE_TTL, TimeUnit.SECONDS);

        // TODO: 실제 메일 발송 로직 (JavaMailSender)
        System.out.println("인증번호 발송 - Email: " + email + ", Code: " + code);
    }

    @Transactional
    public Map<String, String> verifyEmailAndPromote(String email, String code, Member guestMember) {
        return verifyEmailAndIssueTokens(email, code, guestMember);
    }

    @Transactional
    public Map<String, String> verifyEmailAndIssueTokens(String email, String code) {
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .email(email)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .role(MemberRole.USER)
                        .status(MemberStatus.ACTIVE)
                        .build()));

        return verifyEmailAndIssueTokens(email, code, member);
    }

    private Map<String, String> verifyEmailAndIssueTokens(String email, String code, Member guestMember) {
        String savedCode = redisTemplate.opsForValue().get(REDIS_PREFIX + email);

        if (savedCode == null || !savedCode.equals(code)) {
            throw new IllegalArgumentException("인증번호가 일치하지 않거나 만료되었습니다.");
        }

        // 인증 성공, Redis 코드 삭제
        redisTemplate.delete(REDIS_PREFIX + email);

        // 엔티티 업데이트 후 저장
        guestMember.completeRegistration(email);
        memberRepository.save(guestMember);

        // 새로운 USER 권한의 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(guestMember.getEmail(), guestMember.getRole().getSecurityRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(guestMember.getEmail());
        refreshTokenRepository.save(new RefreshToken(
                guestMember.getEmail(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenValidTimeInSeconds()
        ));

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("tokenType", "Bearer");
        tokens.put("expiresIn", "1800");

        return tokens;
    }
}
