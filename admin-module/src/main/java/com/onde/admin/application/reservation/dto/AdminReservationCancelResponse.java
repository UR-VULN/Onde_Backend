package com.onde.admin.application.reservation.dto;

import com.onde.core.entity.reservation.ReservationStatus;

public record AdminReservationCancelResponse(
        Long reservationId,
        ReservationStatus status
) {
}
