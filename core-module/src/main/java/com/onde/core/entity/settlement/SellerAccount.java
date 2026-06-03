package com.onde.core.entity.settlement;

import com.onde.core.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_accounts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SellerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false, length = 50)
    private String bankName;

    @Column(length = 200)
    private String businessName;

    @Column(length = 30)
    private String contactPhone;

    @Column(length = 500)
    private String businessAddress;

    @Column(nullable = false, length = 100)
    private String accountHolder; // 예금주명

    @Column(nullable = false, length = 255) // 암호화된 문자열이 들어가므로 길게 설정
    private String accountNumber; // 계좌번호

    @Column(nullable = false, length = 20, unique = true)
    private String businessNumber; // 사업자등록번호

    @Column(nullable = false, length = 50)
    private String representativeName; // 대표자명

    @Column(nullable = false, length = 8) // 개업일자 (YYYYMMDD)
    private String openedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'") 
    @Builder.Default
    private AccountStatus status = AccountStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void updateAccount(String bankName, String accountNumber, String accountHolder) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }

    public void approveAccount() {
    this.status = AccountStatus.APPROVED;
}
    public void rejectAccount() {
        this.status = AccountStatus.REJECTED;
    }
}
