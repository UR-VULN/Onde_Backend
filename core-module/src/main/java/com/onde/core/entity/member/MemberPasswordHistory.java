package com.onde.core.entity.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_password_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberPasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private MemberPasswordHistory(Long memberId, String passwordHash, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    @NonNull
    public static MemberPasswordHistory of(Long memberId, String passwordHash) {
        return new MemberPasswordHistory(memberId, passwordHash, LocalDateTime.now());
    }
}
