package com.onde.api.application.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {
    @JsonIgnore
    private String accessToken;
    @JsonIgnore
    private String refreshToken;
    private String tokenType;  // "Bearer"
    private Long expiresIn;    // 1800 (초)
    private Long memberId;
    private String role;
}