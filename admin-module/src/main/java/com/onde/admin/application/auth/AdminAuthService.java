package com.onde.admin.application.auth;

import com.onde.core.entity.member.Member;
import com.onde.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION = 30; // 30분

    @Transactional
    public void changePassword(Long adminId, String rawPassword, String newRawPassword) {
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(rawPassword, admin.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        admin.updatePassword(passwordEncoder.encode(newRawPassword));
        
        String redisKey = "RT:" + admin.getEmail();
        redisTemplate.delete(redisKey);
    }

    public Member authenticate(String email, String rawPassword) {
        String lockKey = "login_lock:" + email;
        String failCountKey = "login_fail_count:" + email;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            log.warn("잠긴 계정 로그인 시도 감지: {}", email);
            throw new IllegalStateException("보안 경고: 비밀번호 5회 오류로 계정이 30분간 잠겼습니다.");
        }

        Member admin = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 틀렸습니다."));


        if (!passwordEncoder.matches(rawPassword, admin.getPassword())) {
            Long attempts = redisTemplate.opsForValue().increment(failCountKey);
            log.info("로그인 실패: {} (현재 실패 횟수: {})", email, attempts);

            if (attempts != null && attempts == 1) {
                redisTemplate.expire(failCountKey, Duration.ofMinutes(LOCK_TIME_DURATION));
            }

            if (attempts != null && attempts >= MAX_LOGIN_ATTEMPTS) {
                redisTemplate.opsForValue().set(lockKey, "LOCKED", Duration.ofMinutes(LOCK_TIME_DURATION));
                redisTemplate.delete(failCountKey);
                log.warn("계정 잠금 처리됨: {}", email);
                throw new IllegalStateException("보안 경고: 비밀번호 5회 오류로 계정이 30분간 잠겼습니다.");
            }

            throw new BadCredentialsException("이메일 또는 비밀번호가 틀렸습니다.");

        }

        log.info("로그인 성공: {}", email);
        redisTemplate.delete(failCountKey);

        return admin;
    }
}