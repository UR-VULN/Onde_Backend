package com.onde.api.application.accommodation.dto;

import com.onde.core.entity.reservation.ReservationStatus;
import com.onde.core.entity.reservation.ReservationTarget;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CarReservationResponse(
        Long reservationId,
        ReservationTarget targetType,
        Long targetId,
        String modelName,
        LocalDate pickupDate,
        LocalDate returnDate,
        BigDecimal totalPrice,
        ReservationStatus status
) {
}
