package com.onde.api.application.accommodation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AccommodationSearchResponse {
    private List<AccommodationListDto> accommodations;
    private Integer totalCount;
}
