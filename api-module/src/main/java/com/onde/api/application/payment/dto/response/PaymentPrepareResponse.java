package com.onde.api.application.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 결제 사전 검증 응답 DTO
 */
@Getter
@Builder
public class PaymentPrepareResponse {

    /** 가맹점 주문번호 */
    private String merchantUid;

    /** 청구 결제 금액 */
    private BigDecimal pgAmount;

    /** 사용 마일리지 */
    private Integer usedMileage;

    /** 예약 식별자 */
    private Long reservationId;
}
