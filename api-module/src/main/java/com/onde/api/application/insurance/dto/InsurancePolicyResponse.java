package com.onde.api.application.insurance.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class InsurancePolicyResponse {
    private String policyCode;
    private String insuredName;
    private String coverageLevel;
    private BigDecimal totalPremium;
    private String status;
}
