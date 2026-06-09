package com.onde.api.application.payment.dto.response;

import lombok.Builder;
import lombok.Getter;
import com.onde.core.entity.payment.PaymentStatus;

import java.math.BigDecimal;

/**
 * 결제 사후 검증 응답 DTO
 */
@Getter
@Builder
public class PaymentValidateResponse {

    /** 결제 식별자 */
    private Long paymentId;

    /** 결제 고유 거래 ID */
    private String impUid;

    /** 총 주문 금액 */
    private BigDecimal totalAmount;

    /** 실제로 승인 처리된 지갑 결제 금액 */
    private BigDecimal pgAmount;

    /** 사용 마일리지 금액 */
    private Integer usedMileage;

    /** 적립 마일리지 금액 */
    private Integer accumulatedMileage;

    /** 결제 상태 */
    private PaymentStatus status;
}
