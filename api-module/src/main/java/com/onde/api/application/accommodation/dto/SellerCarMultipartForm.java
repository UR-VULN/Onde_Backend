package com.onde.api.application.accommodation.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerCarMultipartForm {

    @NotBlank(message = "licensePlate은 필수입니다.")
    @Size(max = ValidationLimits.LICENSE_PLATE_MAX, message = "차량 번호는 20자 이하여야 합니다.")
    private String licensePlate;

    @NotBlank(message = "modelName은 필수입니다.")
    @Size(max = ValidationLimits.MODEL_NAME_MAX, message = "모델명은 100자 이하여야 합니다.")
    private String modelName;

    @NotBlank(message = "carType은 필수입니다.")
    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "차량 유형은 500자 이하여야 합니다.")
    private String carType;

    @Pattern(regexp = "^$|^\\d{1,9}$", message = "일일 요금 형식이 올바르지 않습니다.")
    private String dailyPrice;

    @Size(max = ValidationLimits.ADDRESS_MAX, message = "위치는 500자 이하여야 합니다.")
    private String location;
}
