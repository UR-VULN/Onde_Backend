package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.BookingStatus;
import com.onde.core.entity.flight.SeatClass;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FlightBookingResponse {
    private String bookingCode;
    private String passengerName;
    private SeatClass seatClass;
    private BigDecimal totalPrice;
    private BookingStatus status;
}
