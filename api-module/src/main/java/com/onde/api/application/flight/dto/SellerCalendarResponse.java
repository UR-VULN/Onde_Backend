package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.ApprovalStatus;
import com.onde.core.entity.flight.SeatClass;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SellerCalendarResponse {
    private Long scheduleId;
    private String flightNumber;
    private String departureAirport;
    private String arrivalAirport;
    private LocalDateTime departureTime;
    private SeatClass classType;
    private Integer totalSeats;
    private Integer remainingSeats;
    private BigDecimal basePrice;
    private ApprovalStatus status;
}
