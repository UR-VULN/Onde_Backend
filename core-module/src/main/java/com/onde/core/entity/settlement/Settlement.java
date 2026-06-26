package com.onde.core.entity.settlement;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일별 판매자 정산 데이터를 관리하는 엔티티입니다.
 * 판매자의 일간 총 매출액(grossAmount)에서 플랫폼 중개 수수료(commission)를 공제한
 * 순 지급액(netAmount)을 집계하여 정산 단계를 추적합니다.
 */
@Entity
@Table(name = "settlements")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Settlement {

    /**
     * 정산 고유 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 정산 대상 판매자 ID (FK → members.id 논리)
     */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    /**
     * 정산 기준 (매일 기준 적재)
     */
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    /**
     * 정산 달의 총 거래액 (이용 완료 기준)
     */
    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    /**
     * 플랫폼 운영 수수료 (기본 3% 산출)
     */
    @Column(name = "commission", nullable = false, precision = 14, scale = 2)
    private BigDecimal commission;

    /**
     * 최종 정산 지급액 (gross_amount - commission)
     */
    @Column(name = "net_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount;

    /**
     * PENDING / REQUESTED / APPROVED_1ST / COMPLETED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    /**
     * 내역 생성 일시
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 처리 및 수정 일시
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 판매자의 정산 지급 신청 일시 (상태가 REQUESTED로 변할 때 기록)
     */
    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    /**
     * 본사 정산 담당자가 1차 승인한 일시 (APPROVED_1ST 시점)
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * 최고 관리자가 최종 지급 확정 및 이체를 완료한 일시 (COMPLETED 시점)
     */
    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @Version
    private Long version;
}

