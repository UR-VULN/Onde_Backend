package com.onde.api.application.accommodation.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CarReservationRequest {

    @NotNull(message = "carId는 필수입니다.")
    @Min(value = 1, message = "carId 형식이 올바르지 않습니다.")
    private Long carId;

    /** 클라이언트 전송 금지 — JWT 주체와 불일치 시 403 */
    @Min(value = 1, message = "memberId 형식이 올바르지 않습니다.")
    private Long memberId;

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "보험 유형은 500자 이하여야 합니다.")
    private String insuranceType;

    private LocalDate pickupDate;
    private LocalDate returnDate;
    private LocalDate startDate;
    private LocalDate endDate;

    @Min(value = 0, message = "총 금액은 0원 이상이어야 합니다.")
    @Max(value = ValidationLimits.AMOUNT_MAX, message = "총 금액이 허용 범위를 초과합니다.")
    private Integer totalPrice;

    public LocalDate getStartDate() {
        return startDate != null ? startDate : pickupDate;
    }

    public LocalDate getEndDate() {
        return endDate != null ? endDate : returnDate;
    }
}
