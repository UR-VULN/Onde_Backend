package com.onde.api.application.insurance.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class InsuranceCalculateRequest {

    @Min(value = 1, message = "상품 ID 형식이 올바르지 않습니다.")
    private Long productId;

    @Min(value = 1, message = "상품 ID 형식이 올바르지 않습니다.")
    private Long insuranceProductId;

    private LocalDate birthdate;

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "목적지는 500자 이하여야 합니다.")
    private String destination;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    @Pattern(regexp = "^(STANDARD|DELUXE|PREMIUM)$", message = "coverageLevel은 STANDARD, DELUXE, PREMIUM만 허용됩니다.")
    private String coverageLevel;

    public Long getInsuranceProductId() {
        return insuranceProductId != null ? insuranceProductId : productId;
    }
}
