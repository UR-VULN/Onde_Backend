package com.onde.api.application.auth.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh Token은 필수 입력값입니다.")
    @Size(max = ValidationLimits.TOKEN_MAX, message = "토큰 형식이 올바르지 않습니다.")
    private String refreshToken;
}
