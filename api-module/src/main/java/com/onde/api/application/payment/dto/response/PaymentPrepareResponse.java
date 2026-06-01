package com.onde.api.application.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * PG 결제 사전 등록(검증 준비) 완료 정보를 반환하는 DTO 클래스입니다.
 */
@Getter
@Builder
public class PaymentPrepareResponse {

    /**
     * 백엔드에서 사전 생성한 고유 주문번호. 프론트엔드는 이 값을 포트원 merchant_uid 파라미터로 넘겨야 합니다.
     */
    private String merchantUid;

    /**
     * 실제 PG사(카드사 등) 결제창에 청구해야 하는 현금성 결제 금액 (totalAmount - usedMileage)
     */
    private BigDecimal pgAmount;

    /**
     * 이 결제 건에 적용되어 사전 차감 처리될 마일리지 액수
     */
    private Integer usedMileage;

    /**
     * 결제 대상 예약 식별자 (PK)
     */
    private Long reservationId;
}
