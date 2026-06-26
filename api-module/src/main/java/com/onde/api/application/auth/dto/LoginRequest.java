package com.onde.api.application.auth.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    public static final String CREDENTIALS_INVALID_MESSAGE = "이메일 또는 비밀번호가 올바르지 않습니다.";

    @NotBlank(message = CREDENTIALS_INVALID_MESSAGE)
    @Email(message = CREDENTIALS_INVALID_MESSAGE)
    @Size(max = ValidationLimits.EMAIL_MAX, message = CREDENTIALS_INVALID_MESSAGE)
    private String email;

    @NotBlank(message = CREDENTIALS_INVALID_MESSAGE)
    @Size(max = ValidationLimits.PASSWORD_MAX, message = CREDENTIALS_INVALID_MESSAGE)
    private String password;
}
