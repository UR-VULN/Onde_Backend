package com.onde.core.security;

import com.onde.core.config.LoginLockProperties;
import com.onde.core.entity.member.Member;
import com.onde.core.exception.BusinessException;
import com.onde.core.exception.ErrorCode;
import com.onde.core.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final MemberRepository memberRepository;
    private final LoginLockProperties loginLockProperties;

    public void assertNotLocked(Member member) {
        Long memberId = requireMemberId(member.getId());
        clearExpiredLockIfNeeded(memberId);
        Member current = memberRepository.findById(memberId).orElse(member);
        if (current.isLoginLocked()) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(@NonNull Long memberId) {
        Member member = memberRepository.findById(requireMemberId(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.incrementFailedLoginAttempts();
        if (member.getFailedLoginAttempts() >= loginLockProperties.getMaxFailedAttempts()) {
            member.lockLoginUntil(LocalDateTime.now().plusMinutes(loginLockProperties.getLockMinutes()));
        }
        memberRepository.save(member);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(@NonNull Long memberId) {
        Member member = memberRepository.findById(requireMemberId(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.clearLoginFailures();
        memberRepository.save(member);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearExpiredLockIfNeeded(@NonNull Long memberId) {
        Member member = memberRepository.findById(requireMemberId(memberId)).orElse(null);
        if (member == null) {
            return;
        }
        if (member.getLoginLockedUntil() == null) {
            return;
        }
        if (member.getLoginLockedUntil().isAfter(LocalDateTime.now())) {
            return;
        }
        member.clearLoginFailures();
        memberRepository.save(member);
    }

    @NonNull
    private static Long requireMemberId(Long memberId) {
        return Objects.requireNonNull(memberId, "memberId must not be null");
    }
}
