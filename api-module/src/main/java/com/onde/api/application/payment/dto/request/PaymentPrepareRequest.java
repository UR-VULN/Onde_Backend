package com.onde.api.application.payment.dto.request;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentPrepareRequest {

    @NotNull(message = "예약 ID는 필수입니다.")
    @Min(value = 1, message = "예약 ID 형식이 올바르지 않습니다.")
    private Long reservationId;

    @Size(max = 20, message = "예약 타입 형식이 올바르지 않습니다.")
    private String reservationType;

    @Min(value = 0, message = "마일리지는 0 이상이어야 합니다.")
    @Max(value = ValidationLimits.MILEAGE_MAX, message = "마일리지가 허용 범위를 초과합니다.")
    private Integer usedMileage;

    @NotNull(message = "결제 금액은 필수입니다.")
    @DecimalMin(value = "0", message = "결제 금액은 0원 이상이어야 합니다.")
    @DecimalMax(value = "999999999", message = "결제 금액이 허용 범위를 초과합니다.")
    private BigDecimal totalAmount;
}
