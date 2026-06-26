package com.onde.api.application.accommodation.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerAccommodationMultipartForm {

    @NotBlank(message = "숙소명은 필수입니다.")
    @Size(max = ValidationLimits.TITLE_MAX, message = "숙소명은 200자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "설명은 필수입니다.")
    @Size(max = ValidationLimits.DESCRIPTION_MAX, message = "설명은 5,000자 이하여야 합니다.")
    private String description;

    @NotBlank(message = "카테고리는 필수입니다.")
    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "카테고리는 500자 이하여야 합니다.")
    private String category;

    @NotBlank(message = "위치는 필수입니다.")
    @Size(max = ValidationLimits.ADDRESS_MAX, message = "위치는 500자 이하여야 합니다.")
    private String location;

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "사업자 등록번호는 500자 이하여야 합니다.")
    private String businessLicense;

    @Min(value = -90, message = "위도 형식이 올바르지 않습니다.")
    @Max(value = 90, message = "위도 형식이 올바르지 않습니다.")
    private Double latitude;

    @Min(value = -180, message = "경도 형식이 올바르지 않습니다.")
    @Max(value = 180, message = "경도 형식이 올바르지 않습니다.")
    private Double longitude;

    /** JSON 문자열 (객실 목록) */
    @Size(max = ValidationLimits.CONTENT_MAX, message = "객실 정보 JSON이 너무 깁니다.")
    private String rooms;
}
