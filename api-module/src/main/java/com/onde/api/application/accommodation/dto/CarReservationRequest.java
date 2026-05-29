package com.onde.api.application.accommodation.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
public class CarReservationRequest {
    private Long memberId;
    private Long carId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalPrice;
}
