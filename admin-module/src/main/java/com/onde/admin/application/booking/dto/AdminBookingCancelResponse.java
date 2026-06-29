package com.onde.admin.application.booking.dto;

import com.onde.core.entity.flight.BookingStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AdminBookingCancelResponse {
    private String bookingCode;
    private BookingStatus status;
}
