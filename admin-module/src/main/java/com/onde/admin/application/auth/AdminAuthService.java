package com.onde.admin.application.auth;

import com.onde.core.entity.member.Member;
import com.onde.core.repository.MemberRepository;
import com.onde.core.security.AuthSessionRevocationService;
import com.onde.core.security.PasswordLifecycleService;
import com.onde.core.validation.PasswordPolicyLevel;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionRevocationService authSessionRevocationService;
    private final PasswordLifecycleService passwordLifecycleService;

    @Transactional
    public void changePassword(Long adminId, String rawPassword, String newRawPassword) {
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(rawPassword, admin.getPassword())) {
            throw new UnauthorizedException(ErrorCode.LOGIN_CREDENTIALS_INVALID);
        }

        passwordLifecycleService.changePassword(admin, newRawPassword, PasswordPolicyLevel.ADMIN);
        authSessionRevocationService.revokeAllSessions(admin);
    }
}
