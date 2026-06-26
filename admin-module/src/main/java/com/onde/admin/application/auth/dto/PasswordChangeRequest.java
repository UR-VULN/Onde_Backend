package com.onde.admin.application.auth.dto;

import com.onde.core.validation.PasswordPolicyLevel;
import com.onde.core.validation.ValidationLimits;
import com.onde.core.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeRequest {

    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    @Size(max = ValidationLimits.PASSWORD_MAX, message = "현재 비밀번호 형식이 올바르지 않습니다.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @ValidPassword(level = PasswordPolicyLevel.ADMIN)
    private String newPassword;
}
