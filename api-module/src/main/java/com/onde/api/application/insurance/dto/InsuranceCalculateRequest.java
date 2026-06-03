package com.onde.api.application.insurance.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class InsuranceCalculateRequest {
    private Long productId;
    private Long insuranceProductId;
    private LocalDate birthdate;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String coverageLevel; // STANDARD, DELUXE, PREMIUM

    public Long getInsuranceProductId() {
        return insuranceProductId != null ? insuranceProductId : productId;
    }
}
