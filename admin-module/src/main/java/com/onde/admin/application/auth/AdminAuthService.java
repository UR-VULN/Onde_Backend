package com.onde.admin.application.auth;

import com.onde.core.entity.member.Member;
import com.onde.core.repository.MemberRepository;
import com.onde.core.security.PasswordLifecycleService;
import com.onde.core.validation.PasswordPolicyLevel;
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
    private final PasswordLifecycleService passwordLifecycleService;

    @Transactional
    public void changePassword(Long adminId, String rawPassword, String newRawPassword) {
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(rawPassword, admin.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        passwordLifecycleService.changePassword(admin, newRawPassword, PasswordPolicyLevel.ADMIN);

        // 보안을 위해 기존 Refresh Token 삭제
        String redisKey = "RT:" + admin.getEmail();
        redisTemplate.delete(redisKey);
    }
}
