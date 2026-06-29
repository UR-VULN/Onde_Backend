package com.onde.api.application.flight.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SellerFlightRegisterResponse {
    private java.util.List<Long> registeredScheduleIds;
    private String batchGroupId;
    private Integer createdCount;
    private String status; // PENDING_APPROVAL
}
