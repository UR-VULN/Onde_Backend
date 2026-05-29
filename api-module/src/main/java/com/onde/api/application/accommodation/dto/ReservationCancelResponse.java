package com.onde.api.application.accommodation.dto;

import com.onde.core.entity.reservation.ReservationStatus;

public record ReservationCancelResponse(
    Long reservationId,
    ReservationStatus status, // CANCELLED 상태 등을 반환
    String message
) {}