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
public class InsurancePolicyResponse {
    private Long policyId;
    private String policyCode;
    private String productName;
    private String insuredName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String coverageLevel;
    private BigDecimal totalPremium;
    private String status;
}
