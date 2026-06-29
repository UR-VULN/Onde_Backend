package com.onde.api.application.auth.dto;

import com.onde.core.entity.member.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {
    
    private MemberRole role; 
    
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    private String name;
    
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\[\\]{};':\",./<>?~`|\\\\-])[A-Za-z\\d!@#$%^&*()_+=\\[\\]{};':\",./<>?~`|\\\\-]{8,20}$", message = "비밀번호는 영문 대문자, 소문자, 숫자, 특수문자를 모두 포함하여 8~20자이어야 합니다.")
    private String password;
    
    private String passwordConfirm;
    
    private String phoneNumber;

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    private String nickname;

    private Integer age;
}
