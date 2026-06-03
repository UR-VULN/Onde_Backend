package com.onde.core.entity.payment;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 내역을 저장하는 엔티티입니다.
 * 일반 PG사 결제와 마일리지를 함께 사용하는 복합 결제를 지원하도록 설계되었습니다.
 */
@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    /**
     * 결제 내역 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 결제를 수행한 회원의 식별자 (FK 역할)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 이 결제와 연관된 예약의 식별자 (FK 역할 - reservations 또는 flight_bookings)
     */
    @Column(name = "reservation_id")
    private Long reservationId;

    /**
     * 사용자가 최종적으로 지불해야 하는 상품/예약의 총 주문 금액
     */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 실제 PG사(신용카드, 간편결제 등)를 통해 청구 및 승인된 금액
     * 계산 공식: pgAmount = totalAmount - usedMileage
     */
    @Column(name = "pg_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal pgAmount;

    /**
     * 이 결제 시 차감하여 사용한 마일리지 금액
     */
    @Column(name = "used_mileage", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer usedMileage = 0;

    /**
     * 이 결제 완료를 통해 새로이 사용자에게 적립된 마일리지 금액
     */
    @Column(name = "accumulated_mileage", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer accumulatedMileage = 0;

    /**
     * 포트원(PortOne, 구 아임포트)에서 발급하는 고유 거래 식별자 (환불 시 사용)
     */
    @Column(name = "imp_uid", length = 100, unique = true)
    private String impUid;

    /**
     * 우리 서비스(가맹점)에서 생성한 고유 주문 ID
     */
    @Column(name = "merchant_uid", length = 100, nullable = false, unique = true)
    private String merchantUid;

    /**
     * 결제와 연관된 예약 서비스 종류 (예: ROOM, CAR, FLIGHT, INSURANCE 등)
     * 통계 및 매출 비중 집계 시 이용됩니다.
     */
    @Column(name = "reservation_type", nullable = false, length = 20)
    private String reservationType;

    /**
     * 결제 상태 (PAID: 완료, CANCELLED: 취소, REFUNDED: 환불)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private PaymentStatus status;

    /**
     * 결제 내역이 생성된 일시 (자동 등록)
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 결제 내역이 최종 수정된 일시 (자동 갱신)
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
