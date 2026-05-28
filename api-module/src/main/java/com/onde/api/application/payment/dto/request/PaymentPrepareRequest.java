package com.onde.api.application.payment.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * PG 결제 사전 등록(검증 준비)을 요청할 때 사용되는 DTO 클래스입니다.
 */
@Getter
@Setter
public class PaymentPrepareRequest {

    /**
     * 결제 대상이 되는 예약 식별자 (PK)
     */
    private Long reservationId;

    /**
     * 예약의 종류/범주 (예: ROOM, CAR, FLIGHT, INSURANCE 등)
     */
    private String reservationType;

    /**
     * 이번 결제에서 사용하겠다고 프론트엔드에서 입력한 마일리지 금액
     */
    private Long usedMileage;

    /**
     * 마일리지 적용 전, 결제되어야 할 총 상품/예약 원금액
     */
    private Long totalAmount;
}
