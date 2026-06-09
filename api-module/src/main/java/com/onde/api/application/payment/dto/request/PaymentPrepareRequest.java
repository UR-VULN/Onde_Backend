package com.onde.api.application.payment.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 결제 사전 검증 요청 DTO
 */
@Getter
@Setter
public class PaymentPrepareRequest {

    /** 예약 식별자 */
    private Long reservationId;

    /** 예약 타입 (ROOM, CAR, FLIGHT, INSURANCE) */
    private String reservationType;

    /** 사용 마일리지 */
    private Integer usedMileage;

    /** 결제 총 금액 */
    private BigDecimal totalAmount;
}
