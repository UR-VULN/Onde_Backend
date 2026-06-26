package com.onde.api.application.insurance.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class InsurancePolicyRequest {

    @Min(value = 1, message = "상품 ID 형식이 올바르지 않습니다.")
    private Long productId;

    @Min(value = 1, message = "상품 ID 형식이 올바르지 않습니다.")
    private Long insuranceProductId;

    @NotBlank(message = "피보험자명은 필수입니다.")
    @Size(max = ValidationLimits.NAME_MAX, message = "피보험자명은 100자 이하여야 합니다.")
    private String insuredName;

    @NotNull(message = "피보험자 생년월일은 필수입니다.")
    private LocalDate insuredBirthdate;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    @Pattern(regexp = "^(STANDARD|DELUXE|PREMIUM)$", message = "coverageLevel은 STANDARD, DELUXE, PREMIUM만 허용됩니다.")
    private String coverageLevel;

    @NotNull(message = "보험료는 필수입니다.")
    @DecimalMin(value = "0", message = "보험료는 0원 이상이어야 합니다.")
    @DecimalMax(value = "999999999", message = "보험료가 허용 범위를 초과합니다.")
    private BigDecimal totalPremium;

    public Long getInsuranceProductId() {
        return insuranceProductId != null ? insuranceProductId : productId;
    }
}
