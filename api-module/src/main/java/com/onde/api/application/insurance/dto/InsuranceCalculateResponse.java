package com.onde.api.application.insurance.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class InsuranceCalculateResponse {
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
