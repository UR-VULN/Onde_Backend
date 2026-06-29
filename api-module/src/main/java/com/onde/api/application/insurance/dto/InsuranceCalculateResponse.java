package com.onde.api.application.insurance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InsuranceCalculateResponse {
    private Long productId;
    private String productName;
    private Integer travelDays;
    private BigDecimal baseDailyRate;
    private BigDecimal totalPremium;
    private Object coverageDetails;
    private Long insuranceProductId;
    private Integer tripDurationDays;
    private Integer age;
    private BigDecimal ageMultiplier;
    private String coverageLevel;
    private BigDecimal coverageMultiplier;
    private BigDecimal calculatedPremium;
    private BreakdownDto breakdown;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class BreakdownDto {
        private BigDecimal baseDailyRate;
        private BigDecimal basePremiumWithoutMultipliers;
    }
}
