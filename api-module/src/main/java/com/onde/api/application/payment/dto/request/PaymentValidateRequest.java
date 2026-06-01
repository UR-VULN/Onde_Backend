package com.onde.api.application.payment.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * PG사 결제 완료 후, 백엔드 서버에 결제 사후 검증 및 최종 승인을 요청하는 DTO 클래스입니다.
 */
@Getter
@Setter
public class PaymentValidateRequest {

    /**
     * 포트원에서 발급받은 결제 고유 거래 ID (환불 시 API 통신용으로 사용됨)
     */
    private String impUid;

    /**
     * 가맹점에서 사전 생성했던 주문번호 (merchant_uid)
     */
    private String merchantUid;

    /**
     * 실제 PG사 결제창을 통해 성공적으로 승인 및 결제 완료된 현금성 금액 (사후 대조용)
     */
    private BigDecimal pgAmount;
}
