package com.onde.admin.application.member.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlacklistRequest {

    @Size(max = ValidationLimits.GENERIC_TEXT_MAX, message = "블랙리스트 사유는 500자 이하여야 합니다.")
    private String reason;
}
