package com.onde.api.application.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RefreshSessionResult {
    private String accessToken;
    private TokenRefreshResponse profile;
}
