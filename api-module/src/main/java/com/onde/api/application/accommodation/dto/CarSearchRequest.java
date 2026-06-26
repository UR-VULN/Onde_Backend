package com.onde.api.application.accommodation.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class CarSearchRequest {

    @Size(max = ValidationLimits.ADDRESS_MAX, message = "지역명은 500자 이하여야 합니다.")
    private String location;

    private LocalDate pickupDate;
    private LocalDate returnDate;

    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime pickup;

    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime returnTime;

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "차량 유형은 500자 이하여야 합니다.")
    private String carType;

    @Size(max = 20, message = "정렬 형식이 올바르지 않습니다.")
    private String sort;

    public LocalDateTime getPickup() {
        return pickup != null ? pickup : (pickupDate != null ? pickupDate.atStartOfDay() : null);
    }

    public LocalDateTime getReturnTime() {
        return returnTime != null ? returnTime : (returnDate != null ? returnDate.atStartOfDay() : null);
    }
}
