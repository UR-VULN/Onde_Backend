package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.SeatClass;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FlightBookingRequest {
    private Long scheduleId;
    private SeatClass seatClass;
    private String passengerName;
    private String passengerPassport;
    private LocalDate passengerBirthdate;
    private BigDecimal totalPrice;
}
