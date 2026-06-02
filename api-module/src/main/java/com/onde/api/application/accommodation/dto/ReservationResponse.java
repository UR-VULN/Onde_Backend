package com.onde.api.application.accommodation.dto;

import com.onde.core.entity.reservation.ReservationStatus; // Status 객체 import
import com.onde.core.entity.reservation.ReservationTarget;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// class 대신 record를 사용하면 Getter, 생성자를 자동으로 만들어 줍니다.
public record ReservationResponse(
    Long reservationId,
    ReservationTarget targetType,
    Long targetId,
    LocalDateTime checkIn,
    LocalDateTime checkOut,
    BigDecimal totalPrice,
    ReservationStatus status
) {}
