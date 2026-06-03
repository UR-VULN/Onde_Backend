package com.onde.api.application.accommodation.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
public class CarReservationRequest {
    private Long memberId;
    private Long carId;
    private String insuranceType;
    private LocalDate pickupDate;
    private LocalDate returnDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalPrice;

    public LocalDate getStartDate() {
        return startDate != null ? startDate : pickupDate;
    }

    public LocalDate getEndDate() {
        return endDate != null ? endDate : returnDate;
    }
}
