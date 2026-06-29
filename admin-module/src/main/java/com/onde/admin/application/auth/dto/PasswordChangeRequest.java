package com.onde.admin.application.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeRequest {

    @NotBlank(message = "현재 비밀번호는 필수 입력값입니다.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호는 필수 입력값입니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\[\\]{};':\",./<>?~`|\\\\-])[A-Za-z\\d!@#$%^&*()_+=\\[\\]{};':\",./<>?~`|\\\\-]{10,20}$", message = "비밀번호는 영문 대문자, 소문자, 숫자, 특수문자를 모두 포함하여 10~20자이어야 합니다.")
    private String newPassword;
}