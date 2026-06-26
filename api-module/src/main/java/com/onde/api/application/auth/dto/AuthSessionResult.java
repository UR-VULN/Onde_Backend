package com.onde.api.application.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthSessionResult {
    private LoginResponse profile;
    private String accessToken;
    private String refreshToken;
}
