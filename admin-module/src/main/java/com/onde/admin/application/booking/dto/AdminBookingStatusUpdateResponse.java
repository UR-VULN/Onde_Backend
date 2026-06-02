package com.onde.admin.application.booking.dto;

import com.onde.core.entity.flight.BookingStatus;
import java.time.LocalDateTime;

public record AdminBookingStatusUpdateResponse(
        Long bookingId,
        String bookingCode,
        BookingStatus previousStatus,
        BookingStatus currentStatus,
        LocalDateTime updatedAt
) {
}
