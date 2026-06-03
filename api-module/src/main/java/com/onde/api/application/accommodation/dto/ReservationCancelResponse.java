package com.onde.api.application.accommodation.dto;

import com.onde.core.entity.reservation.ReservationStatus;
import java.time.LocalDateTime;

public record ReservationCancelResponse(
    Long reservationId,
    ReservationStatus status,
    LocalDateTime cancelledAt
) {}
