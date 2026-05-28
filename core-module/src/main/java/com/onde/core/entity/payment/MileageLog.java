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
     * 마일리지 로그 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 마일리지가 변동된 사용자의 식별자 (FK 역할)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 마일리지 변동 금액
     * 양수(+): 적립 (예: 결제 완료에 따른 적립, 환불로 인한 복구)
     * 음수(-): 차감 (예: 결제 시 사용, 적립 취소로 인한 회수)
     */
    @Column(nullable = false)
    private Integer amount;

    /**
     * 마일리지 변동 유형 (적립, 사용, 복구, 회수 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 10)
    private MileageLogType logType;

    /**
     * 마일리지 변동에 대한 상세 설명 (예: "결제 사용", "예약 취소 복구" 등)
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 마일리지 로그가 생성된 일시 (자동 등록)
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

