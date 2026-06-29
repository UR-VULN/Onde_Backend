package com.onde.admin.application.approval.dto;

import com.onde.core.entity.flight.ApprovalStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AdminPendingApprovalsResponse {
    private List<PendingFlightDto> pendingFlights;
    private List<PendingInsuranceDto> pendingInsurances;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class PendingFlightDto {
        private Long scheduleId;
        private String flightNumber;
        private String departureAirport;
        private String arrivalAirport;
        private LocalDateTime departureTime;
        private ApprovalStatus status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class PendingInsuranceDto {
        private Long productId;
        private String productName;
        private BigDecimal baseDailyRate;
        private ApprovalStatus status;
    }
}
