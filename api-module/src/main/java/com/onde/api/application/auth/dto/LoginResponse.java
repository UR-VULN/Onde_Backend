package com.onde.api.application.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;  // "Bearer"
    private Long expiresIn;    // 1800 (초)
    private Long memberId;
    private String role;
}