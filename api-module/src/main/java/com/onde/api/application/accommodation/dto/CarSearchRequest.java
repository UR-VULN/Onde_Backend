package com.onde.api.application.accommodation.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
public class CarSearchRequest {
    private String location;
    private LocalDate pickupDate;
    private LocalDate returnDate;
    
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime pickup;
    
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime returnTime;
    private String carType;
    private String sort;

    public LocalDateTime getPickup() {
        return pickup != null ? pickup : (pickupDate != null ? pickupDate.atStartOfDay() : null);
    }

    public LocalDateTime getReturnTime() {
        return returnTime != null ? returnTime : (returnDate != null ? returnDate.atStartOfDay() : null);
    }
}
