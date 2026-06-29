package com.onde.api.application.accommodation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccommodationListDto {
    private Long accommodationId;
    private Long id;
    private String name;
    private String category;
    private String location;
    private String thumbnailUrl;
    private Integer minPrice;
    private Integer availableRooms;
}
