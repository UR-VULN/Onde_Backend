package com.onde.api.application.accommodation.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SellerAccommodationRegisterRequest {

    @NotBlank(message = "숙소명은 필수입니다.")
    @Size(max = ValidationLimits.TITLE_MAX, message = "숙소명은 200자 이하여야 합니다.")
    private String name;

    @Size(max = ValidationLimits.DESCRIPTION_MAX, message = "설명은 5,000자 이하여야 합니다.")
    private String description;

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "카테고리는 500자 이하여야 합니다.")
    private String category;

    @Size(max = ValidationLimits.ADDRESS_MAX, message = "위치는 500자 이하여야 합니다.")
    private String location;

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "사업자 등록번호는 500자 이하여야 합니다.")
    private String businessLicense;

    @Size(max = ValidationLimits.URL_MAX, message = "썸네일 URL은 2048자 이하여야 합니다.")
    private String thumbnailUrl;

    @Min(value = -90, message = "위도 형식이 올바르지 않습니다.")
    @Max(value = 90, message = "위도 형식이 올바르지 않습니다.")
    private Double latitude;

    @Min(value = -180, message = "경도 형식이 올바르지 않습니다.")
    @Max(value = 180, message = "경도 형식이 올바르지 않습니다.")
    private Double longitude;

    @Valid
    @Size(max = ValidationLimits.LIST_MAX_SIZE, message = "객실 수가 허용 범위를 초과합니다.")
    private List<RoomRegisterRequest> rooms;

    @Getter
    @Setter
    public static class RoomRegisterRequest {

        @NotBlank(message = "객실명은 필수입니다.")
        @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "객실명은 500자 이하여야 합니다.")
        private String name;

        @Min(value = 1, message = "수용 인원은 1명 이상이어야 합니다.")
        @Max(value = ValidationLimits.GUESTS_MAX, message = "수용 인원이 허용 범위를 초과합니다.")
        private Integer capacity;

        @Min(value = 1, message = "기본 인원은 1명 이상이어야 합니다.")
        @Max(value = ValidationLimits.GUESTS_MAX, message = "기본 인원이 허용 범위를 초과합니다.")
        private Integer baseCapacity;

        @DecimalMin(value = "0", message = "추가 인원 요금은 0원 이상이어야 합니다.")
        @DecimalMax(value = "999999999", message = "추가 인원 요금이 허용 범위를 초과합니다.")
        private BigDecimal extraPersonFee;
    }
}
