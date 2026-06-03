package com.onde.admin.application.reservation.dto;

public record AdminReservationStatusUpdateRequest(
        String status,
        String reason
) {
}
