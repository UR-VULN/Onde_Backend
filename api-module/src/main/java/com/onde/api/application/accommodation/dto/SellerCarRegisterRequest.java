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
public class SellerCarRegisterRequest {

    @NotBlank(message = "licensePlate은 필수입니다.")
    @Size(max = ValidationLimits.LICENSE_PLATE_MAX, message = "차량 번호는 20자 이하여야 합니다.")
    private String licensePlate;

    @Size(max = ValidationLimits.MODEL_NAME_MAX, message = "모델명은 100자 이하여야 합니다.")
    private String modelName;

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "차량 유형은 500자 이하여야 합니다.")
    private String carType;

    @Size(max = ValidationLimits.URL_MAX, message = "썸네일 URL은 2048자 이하여야 합니다.")
    private String thumbnailUrl;

    @Min(value = 0, message = "일일 요금은 0원 이상이어야 합니다.")
    @Max(value = ValidationLimits.AMOUNT_MAX, message = "일일 요금이 허용 범위를 초과합니다.")
    private Long dailyPrice;
}
