package com.onde.api.application.insurance.dto;

import com.onde.core.entity.flight.ApprovalStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SellerInsuranceRegisterResponse {
    private Long productId;
    private String productName;
    private ApprovalStatus status;
}
