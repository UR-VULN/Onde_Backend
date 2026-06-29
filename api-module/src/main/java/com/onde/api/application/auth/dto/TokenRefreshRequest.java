package com.onde.api.application.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenRefreshRequest {
    
    @NotBlank(message = "Refresh Token은 필수 입력값입니다.")
    private String refreshToken;
}