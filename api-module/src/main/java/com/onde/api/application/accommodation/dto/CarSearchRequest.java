package com.onde.api.application.accommodation.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class CarSearchRequest {
    private String location;
    private LocalDateTime pickup;
    private LocalDateTime returnTime;
    private String carType;
    private String fuelType;
    private Integer capacity;
    private String sort;
}
