package com.onde.api.application.accommodation.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import jakarta.validation.constraints.PositiveOrZero;

@Getter @Setter
public class CarReservationRequest {
    private Long memberId;
    private Long carId;
    private String insuranceType;
    private LocalDate pickupDate;
    private LocalDate returnDate;
    private LocalDate startDate;
    private LocalDate endDate;
    
    @PositiveOrZero(message = "총 가격은 0 이상이어야 합니다.")
    private Integer totalPrice;

    public LocalDate getStartDate() {
        return startDate != null ? startDate : pickupDate;
    }

    public LocalDate getEndDate() {
        return endDate != null ? endDate : returnDate;
    }
}
