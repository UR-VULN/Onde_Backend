package com.onde.api.application.payment.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 결제 취소 요청 데이터를 전달받는 DTO 클래스입니다.
 */
@Getter
@Setter
public class PaymentCancelRequest {
    
    /**
     * 결제 취소 사유 (예: "고객 변심", "서비스 장애" 등)
     */
    private String reason;
}
