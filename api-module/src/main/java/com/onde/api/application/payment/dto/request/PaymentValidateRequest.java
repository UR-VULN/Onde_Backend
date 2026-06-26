package com.onde.api.application.payment.dto.request;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentValidateRequest {

    @NotBlank(message = "결제 거래 ID는 필수입니다.")
    @Size(max = ValidationLimits.PG_TX_ID_MAX, message = "결제 거래 ID 형식이 올바르지 않습니다.")
    private String impUid;

    @NotBlank(message = "주문번호는 필수입니다.")
    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "주문번호 형식이 올바르지 않습니다.")
    private String merchantUid;

    @DecimalMin(value = "0", message = "결제 금액은 0원 이상이어야 합니다.")
    @DecimalMax(value = "999999999", message = "결제 금액이 허용 범위를 초과합니다.")
    private BigDecimal pgAmount;
}
