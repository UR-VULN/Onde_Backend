package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.BookingStatus;
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
public class FlightBookingResponse {
    private Long bookingId;
    private String bookingCode;
    private Long scheduleId;
    private String flightNumber;
    private String passengerName;
    private SeatClass seatClass;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private LocalDateTime reservedUntil;
}
