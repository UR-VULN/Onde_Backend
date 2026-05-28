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
    private Long insuranceProductId;
    private LocalDate birthdate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String coverageLevel; // STANDARD, DELUXE, PREMIUM
}
