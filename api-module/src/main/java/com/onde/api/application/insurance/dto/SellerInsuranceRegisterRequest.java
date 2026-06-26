package com.onde.api.application.insurance.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SellerInsuranceRegisterRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = ValidationLimits.TITLE_MAX, message = "상품명은 200자 이하여야 합니다.")
    private String productName;

    @DecimalMin(value = "0", message = "기본 일일 요율은 0원 이상이어야 합니다.")
    @DecimalMax(value = "999999999", message = "기본 일일 요율이 허용 범위를 초과합니다.")
    private BigDecimal baseDailyRate;

    @Size(max = ValidationLimits.DESCRIPTION_MAX, message = "보장 상세는 5,000자 이하여야 합니다.")
    private String coverageDetails;
}
