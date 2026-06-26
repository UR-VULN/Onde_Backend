package com.onde.api.application.accommodation.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RoomReservationRequest {

    @NotNull(message = "roomId는 필수입니다.")
    @Min(value = 1, message = "roomId 형식이 올바르지 않습니다.")
    private Long roomId;

    /** 클라이언트 전송 금지 — JWT 주체와 불일치 시 403 */
    @Min(value = 1, message = "memberId 형식이 올바르지 않습니다.")
    private Long memberId;

    private LocalDate checkIn;
    private LocalDate checkOut;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    @NotNull(message = "투숙 인원은 필수입니다.")
    @Min(value = ValidationLimits.GUESTS_MIN, message = "투숙 인원은 1명 이상이어야 합니다.")
    @Max(value = ValidationLimits.GUESTS_MAX, message = "투숙 인원은 20명 이하여야 합니다.")
    private Integer guests;

    public LocalDate getCheckInDate() {
        return checkInDate != null ? checkInDate : checkIn;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate != null ? checkOutDate : checkOut;
    }
}
