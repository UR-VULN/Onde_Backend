package com.onde.api.application.flight.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SellerFlightRegisterResponse {
    private String batchGroupId;
    private Integer createdCount;
    private String status; // PENDING_APPROVAL
}
