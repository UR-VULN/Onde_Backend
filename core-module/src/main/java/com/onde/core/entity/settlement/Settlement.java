package com.onde.core.entity.settlement;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
     * 정산 내역 고유 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 정산 대상이 되는 판매자의 식별자 (FK 역할)
     */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    /**
     * 정산 기준 일자 (해당 일자의 총 거래액에 대한 정산 데이터를 가리킴)
     * 예: 2026년 5월 27일 전체 매출 정산건 -> 2026-05-27로 저장
     */
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    /**
     * 정산 대상 기간(해당 월) 동안 발생한 총 매출액 (수수료 및 마일리지 차감 전 금액)
     */
    @Column(name = "gross_amount", nullable = false)
    private Long grossAmount;

    /**
     * 플랫폼이 취하는 중개 수수료 금액 (기본 요율 등을 곱하여 계산)
     */
    @Column(name = "commission", nullable = false)
    private Long commission;

    /**
     * 판매자에게 실제 지급되어야 하는 최종 정산 금액
     * 공식: netAmount = grossAmount - commission
     */
    @Column(name = "net_amount", nullable = false)
    private Long netAmount;

    /**
     * 정산 진행 상태 (PENDING -> REQUESTED -> APPROVED_1ST -> COMPLETED 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    /**
     * 정산 레코드가 생성된 일시 (자동 등록)
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 정산 정보가 최종 수정(상태 변경 등)된 일시 (자동 갱신)
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

