package com.onde.core.security;

import com.onde.core.entity.member.Member;
import com.onde.core.entity.member.MemberPasswordHistory;
import com.onde.core.entity.member.MemberRole;
import com.onde.core.exception.ErrorCode;
import com.onde.core.exception.ValidationException;
import com.onde.core.repository.MemberPasswordHistoryRepository;
import com.onde.core.validation.PasswordPolicy;
import com.onde.core.validation.PasswordPolicyLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordLifecycleService {

    private final PasswordEncoder passwordEncoder;
    private final MemberPasswordHistoryRepository passwordHistoryRepository;

    @Transactional
    public void changePassword(Member member, String rawNewPassword, PasswordPolicyLevel level) {
        PasswordPolicy.validateOrThrow(rawNewPassword, level);
        assertNotReused(member, rawNewPassword);

        passwordHistoryRepository.deleteByMemberId(member.getId());
        MemberPasswordHistory previousPassword = MemberPasswordHistory.of(member.getId(), member.getPassword());
        passwordHistoryRepository.save(previousPassword);

        member.updatePassword(passwordEncoder.encode(rawNewPassword));
    }

    public void assertNotReused(Member member, String rawNewPassword) {
        if (passwordEncoder.matches(rawNewPassword, member.getPassword())) {
            throw new ValidationException(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
        }
        passwordHistoryRepository.findTopByMemberIdOrderByCreatedAtDesc(member.getId())
                .ifPresent(history -> {
                    if (passwordEncoder.matches(rawNewPassword, history.getPasswordHash())) {
                        throw new ValidationException(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
                    }
                });
    }

    public boolean isPasswordExpired(Member member) {
        LocalDateTime updatedAt = member.getPasswordUpdatedAt();
        if (updatedAt == null) {
            updatedAt = member.getCreatedAt();
        }
        if (updatedAt == null) {
            return false;
        }
        int expiryDays = isAdminRole(member.getRole()) ? PasswordPolicy.ADMIN_EXPIRY_DAYS : PasswordPolicy.USER_EXPIRY_DAYS;
        return updatedAt.plusDays(expiryDays).isBefore(LocalDateTime.now());
    }

    public PasswordPolicyLevel resolveLevel(MemberRole role) {
        return isAdminRole(role) ? PasswordPolicyLevel.ADMIN : PasswordPolicyLevel.USER;
    }

    private boolean isAdminRole(MemberRole role) {
        return role == MemberRole.USER_ADMIN
                || role == MemberRole.SELLER_ADMIN
                || role == MemberRole.SUPER_ADMIN;
    }
}
