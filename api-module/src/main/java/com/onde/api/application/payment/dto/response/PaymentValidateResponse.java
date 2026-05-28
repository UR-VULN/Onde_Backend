package com.onde.api.application.payment.dto.response;

import lombok.Builder;
import lombok.Getter;
import com.onde.core.entity.payment.PaymentStatus;

/**
 * 결제 사후 검증 및 최종 승인 완료 결과 정보를 반환하는 DTO 클래스입니다.
 */
@Getter
@Builder
public class PaymentValidateResponse {

    /**
     * 최종 생성 및 승인된 결제 식별자 (PK)
     */
    private Long paymentId;

    /**
     * 포트원 고유 거래 ID
     */
    private String impUid;

    /**
     * 사용자의 총 주문 금액 (PG 결제액 + 마일리지 사용액)
     */
    private Long totalAmount;

    /**
     * PG사를 통해 실제로 승인 처리된 현금성 결제 금액
     */
    private Long pgAmount;

    /**
     * 이번 결제에서 사용한 마일리지 금액
     */
    private Long usedMileage;

    /**
     * 이번 결제 완료(PG 결제 금액 기준)로 사용자에게 새로이 적립된 마일리지 금액
     */
    private Long accumulatedMileage;

    /**
     * 최종 변경된 결제 상태 (PAID)
     */
    private PaymentStatus status;
}
