package com.onde.api.application.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AccommodationListDto {
    private Long id;
    private String name;
    private String category;
    private String location;
    private String thumbnailUrl;
    private Integer minPrice;
}
