package com.onde.api.application.accommodation.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SellerCarInventoryUpdateRequest {

    @NotNull(message = "carId는 필수입니다.")
    @Min(value = 1, message = "carId 형식이 올바르지 않습니다.")
    private Long carId;

    @Min(value = 0, message = "일일 요금은 0원 이상이어야 합니다.")
    @Max(value = ValidationLimits.AMOUNT_MAX, message = "일일 요금이 허용 범위를 초과합니다.")
    private BigDecimal dailyPrice;

    @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
    @Max(value = 9999, message = "재고 수량이 허용 범위를 초과합니다.")
    private Integer availableCount;
}
