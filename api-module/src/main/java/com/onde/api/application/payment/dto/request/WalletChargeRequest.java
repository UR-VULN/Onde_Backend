package com.onde.api.application.payment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WalletChargeRequest {

    @NotNull(message = "충전 금액은 필수입니다.")
    @DecimalMin(value = "1", message = "충전 금액은 1원 이상이어야 합니다.")
    @DecimalMax(value = "1000000", message = "1회 충전 금액은 1,000,000원을 초과할 수 없습니다.")
    private BigDecimal amount;
}
