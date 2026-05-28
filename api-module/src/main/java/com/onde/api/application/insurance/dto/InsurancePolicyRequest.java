package com.onde.api.application.insurance.dto;

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
    private Long insuranceProductId;
    private String insuredName;
    private LocalDate insuredBirthdate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String coverageLevel;
    private BigDecimal totalPremium;
}
