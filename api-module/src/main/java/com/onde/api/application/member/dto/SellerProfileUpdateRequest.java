package com.onde.api.application.member.dto;

import com.onde.core.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerProfileUpdateRequest {

    private String name;

    @Pattern(regexp = "^$|^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 연락처 형식이 아닙니다. (예: 010-1234-5678)")
    private String phoneNumber;

    @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
    private String nickname;

    @ValidPassword(allowBlank = true)
    private String password;
}
