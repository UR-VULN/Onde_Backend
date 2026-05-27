package com.onde.api.application.accommodation.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class AccommodationSearchRequest {
    private String region;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private Integer starRating;
    private List<String> amenities;
}
