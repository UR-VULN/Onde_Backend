package com.onde.core.entity.member;

import com.onde.core.entity.BaseEntity;
import com.onde.core.security.SkipInputSanitization;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "members",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_provider", columnNames = {"provider", "provider_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_subject_id", nullable = false, unique = true, length = 36)
    private String authSubjectId;

    @Column(length = 100) // email is now nullable and not globally unique
    private String email;

    @Column(nullable = true, length = 100)
    private String name;

    @Column(name = "provider_id", length = 100)
    private String providerId; // 카카오 고유 ID 등 저장

    @SkipInputSanitization
    @Column(nullable = false)
    private String password;

    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @Column(name = "login_locked_until")
    private LocalDateTime loginLockedUntil;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(length = 20)
    private String phoneNumber;

    @Column(unique = true, length = 100)
    private String nickname;

    @Column
    private Integer age;

    // EnumType.STRING을 줘야 DB에 숫자가 아닌 문자로 예쁘게 들어갑니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private MemberRole role;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    // 가입 출처 필드 (기본값 LOCAL 세팅)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'LOCAL'")
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    // --- 비즈니스 편의 메서드 ---
    public void updateRole(MemberRole newRole) {
        this.role = newRole;
    }

    public void updateStatus(MemberStatus newStatus) {
        this.status = newStatus;
    }

    public void updatePassword(String encryptedPassword) {
        this.password = encryptedPassword;
        this.passwordUpdatedAt = LocalDateTime.now();
    }

    /** 신규 가입·OAuth 더미 비밀번호 등 — 암호화된 비밀번호 적용 후 변경 시각 기록 */
    public void markPasswordUpdatedNow() {
        this.passwordUpdatedAt = LocalDateTime.now();
    }

    public boolean isLoginLocked() {
        return loginLockedUntil != null && loginLockedUntil.isAfter(LocalDateTime.now());
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    public void lockLoginUntil(LocalDateTime until) {
        this.loginLockedUntil = until;
        this.failedLoginAttempts = 0;
    }

    public void clearLoginFailures() {
        this.failedLoginAttempts = 0;
        this.loginLockedUntil = null;
    }

    public void completeRegistration(String verifiedEmail) {
        this.email = verifiedEmail;
        this.role = MemberRole.USER;
    }

    public void updateProfile(String name, String phoneNumber, String nickname) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
    }

    /** JWT subject용 비식별 UUID — 없으면 생성 */
    public String ensureAuthSubjectId() {
        if (authSubjectId == null || authSubjectId.isBlank()) {
            authSubjectId = UUID.randomUUID().toString();
        }
        return authSubjectId;
    }
}
