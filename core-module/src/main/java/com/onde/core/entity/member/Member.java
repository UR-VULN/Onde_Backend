package com.onde.core.entity.member;

import com.onde.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String name;

    @Column(length = 20)
    private String phoneNumber;

    // EnumType.STRING을 줘야 DB에 숫자가 아닌 문자로 예쁘게 들어갑니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    // 가입 출처 필드 (기본값 LOCAL 세팅)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    // --- 비즈니스 편의 메서드 ---
    public void updateRole(MemberRole newRole) {
        this.role = newRole;
    }

    public void updateStatus(MemberStatus newStatus) {
        this.status = newStatus;
    }
}