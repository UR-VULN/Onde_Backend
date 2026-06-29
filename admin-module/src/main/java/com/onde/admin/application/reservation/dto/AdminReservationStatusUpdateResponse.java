package com.onde.admin.application.reservation.dto;

import com.onde.core.entity.reservation.ReservationStatus;

import java.time.LocalDateTime;

public record AdminReservationStatusUpdateResponse(
        Long reservationId,
        ReservationStatus previousStatus,
        ReservationStatus currentStatus,
        LocalDateTime updatedAt
) {
}
