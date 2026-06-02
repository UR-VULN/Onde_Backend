package com.onde.admin.application.booking.dto;

public record AdminBookingStatusUpdateRequest(
        String status,
        String reason
) {
}
