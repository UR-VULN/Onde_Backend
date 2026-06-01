package com.onde.api.application.payment.dto.response;

import lombok.Builder;
import lombok.Getter;
import com.onde.core.entity.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 취소 결과 정보를 반환하는 DTO 클래스입니다.
 */
@Getter
@Builder
public class PaymentCancelResponse {

    /**
     * 취소 처리된 결제 식별자 (PK)
     */
    private Long paymentId;

    /**
     * 취소 후의 결제 상태 (일반적으로 CANCELLED)
     */
    private PaymentStatus status;

    /**
     * PG사 측으로 환불(취소) 요청된 현금성 금액 (실제 결제 금액)
     */
    private BigDecimal refundedAmount;

    /**
     * 결제 당시 차감하여 사용했던 마일리지 중 이번 취소로 복구(RESTORE)된 마일리지 액수
     */
    private Integer restoredMileage;

    /**
     * 결제 취소(환불)가 완료된 일시
     */
    private LocalDateTime cancelledAt;
}
