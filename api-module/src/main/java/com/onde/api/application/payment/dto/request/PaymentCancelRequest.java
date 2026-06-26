package com.onde.api.application.payment.dto.request;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCancelRequest {

    @NotBlank(message = "취소 사유는 필수입니다.")
    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "취소 사유는 500자 이하여야 합니다.")
    private String reason;
}
