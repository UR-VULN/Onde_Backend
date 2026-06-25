package com.onde.api.application.payment.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import jakarta.validation.constraints.PositiveOrZero;

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
    @PositiveOrZero(message = "사용 마일리지는 0 이상이어야 합니다.")
    private Integer usedMileage;

    /** 결제 총 금액 */
    @PositiveOrZero(message = "총 결제 금액은 0 이상이어야 합니다.")
    private BigDecimal totalAmount;
}
