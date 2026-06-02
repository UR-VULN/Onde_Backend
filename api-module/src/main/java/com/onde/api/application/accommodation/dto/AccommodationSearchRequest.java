package com.onde.api.application.accommodation.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class AccommodationSearchRequest {
    private String location;
    private String region;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private Integer starRating;
    private String category;
    private String sort;
    private List<String> amenities;

    public String getRegion() {
        return region != null ? region : location;
    }
}
