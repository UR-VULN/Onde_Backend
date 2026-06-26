package com.onde.api.application.accommodation.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AccommodationSearchRequest {

    @Size(max = ValidationLimits.ADDRESS_MAX, message = "지역명은 500자 이하여야 합니다.")
    private String location;

    @Size(max = ValidationLimits.ADDRESS_MAX, message = "지역명은 500자 이하여야 합니다.")
    private String region;

    private LocalDate checkIn;
    private LocalDate checkOut;

    @Min(value = ValidationLimits.GUESTS_MIN, message = "투숙 인원은 1명 이상이어야 합니다.")
    @Max(value = ValidationLimits.GUESTS_MAX, message = "투숙 인원은 20명 이하여야 합니다.")
    private Integer guests;

    @Min(value = 1, message = "별점 형식이 올바르지 않습니다.")
    @Max(value = 5, message = "별점 형식이 올바르지 않습니다.")
    private Integer starRating;

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "카테고리는 500자 이하여야 합니다.")
    private String category;

    @Size(max = 20, message = "정렬 형식이 올바르지 않습니다.")
    private String sort;

    @Size(max = ValidationLimits.LIST_MAX_SIZE, message = "편의시설 목록이 허용 범위를 초과합니다.")
    private List<@Size(max = ValidationLimits.GENERIC_TEXT_MAX) String> amenities;

    @Min(value = ValidationLimits.PAGE_MIN, message = "page 형식이 올바르지 않습니다.")
    private Integer page = 0;

    @Min(value = ValidationLimits.PAGE_SIZE_MIN, message = "size는 1 이상이어야 합니다.")
    @Max(value = ValidationLimits.PAGE_SIZE_MAX, message = "size는 100 이하여야 합니다.")
    private Integer size = 20;

    public String getRegion() {
        return region != null ? region : location;
    }
}
