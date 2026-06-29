package com.onde.api.application.payment.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 결제 사후 검증 요청 DTO
 */
@Getter
@Setter
public class PaymentValidateRequest {

    /** 결제 고유 거래 ID */
    private String impUid;

    /** 가맹점 주문번호 */
    private String merchantUid;

    /** 승인 및 결제 완료 금액 */
    private BigDecimal pgAmount;
}
