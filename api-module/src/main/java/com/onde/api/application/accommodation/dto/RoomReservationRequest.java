package com.onde.api.application.accommodation.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import jakarta.validation.constraints.Positive;

@Getter @Setter
public class RoomReservationRequest {
    private Long memberId;
    private Long roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    
    @Positive(message = "예약 인원은 1명 이상이어야 합니다.")
    private Integer guests;

    public LocalDate getCheckInDate() {
        return checkInDate != null ? checkInDate : checkIn;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate != null ? checkOutDate : checkOut;
    }
}
