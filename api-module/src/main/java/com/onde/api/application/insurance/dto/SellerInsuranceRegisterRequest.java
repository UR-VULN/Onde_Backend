package com.onde.api.application.insurance.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SellerInsuranceRegisterRequest {
    private String productName;
    private BigDecimal baseDailyRate;
    private Object coverageDetails; // JSON object or JSON string
}
