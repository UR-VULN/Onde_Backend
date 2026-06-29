package com.onde.core.entity.payment;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 마일리지 변동 이력을 기록하는 원장(Ledger) 엔티티입니다.
 * 사용자의 현재 마일리지 잔액은 이 테이블의 특정 사용자(userId)에 대한 amount 합산(SUM)을 통해 계산됩니다.
 */
@Entity
@Table(name = "mileage_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MileageLog {

    /**
     * 이력 고유 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 마일리지 변동 대상 회원 (FK → members.id 논리)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 수치 변동량 (양수: 적립 / 음수: 차감)
     */
    @Column(name = "amount", nullable = false)
    private Integer amount;

    /**
     * 마일리지 변동 유형 (EARN / USE / RESTORE / REVOKE)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 10, columnDefinition = "VARCHAR(10)")
    private MileageLogType logType;

    /**
     * 마일리지 적립/차감 상세 사유
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 로그 생성 일시
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false, columnDefinition = "DATETIME DEFAULT NOW()")
    private LocalDateTime createdAt;
}
