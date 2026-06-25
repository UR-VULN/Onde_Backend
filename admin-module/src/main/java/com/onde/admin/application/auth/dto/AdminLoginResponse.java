package com.onde.admin.application.auth.dto;

import lombok.Builder;
import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Builder
public class AdminLoginResponse {
    @JsonIgnore
    private String accessToken;
    @JsonIgnore
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Long memberId;
    private String role;
}
