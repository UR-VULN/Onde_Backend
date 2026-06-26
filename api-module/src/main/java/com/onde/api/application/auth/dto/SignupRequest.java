package com.onde.api.application.auth.dto;



import com.onde.core.entity.member.MemberRole;

import com.onde.core.validation.ValidationLimits;

import com.onde.core.validation.ValidPassword;

import jakarta.validation.constraints.Email;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.Getter;

import lombok.NoArgsConstructor;



@Getter

@NoArgsConstructor

public class SignupRequest {



    private MemberRole role;



    @NotBlank(message = "이메일은 필수 입력값입니다.")

    @Email(message = "올바른 이메일 형식이 아닙니다.")

    @Size(max = ValidationLimits.EMAIL_MAX, message = "이메일은 320자 이하여야 합니다.")

    private String email;



    @Size(max = ValidationLimits.NAME_MAX, message = "이름은 100자 이하여야 합니다.")

    private String name;



    @NotBlank(message = "비밀번호는 필수 입력값입니다.")

    @ValidPassword

    private String password;



    @Size(max = ValidationLimits.PASSWORD_MAX, message = "비밀번호 확인 값이 너무 깁니다.")

    private String passwordConfirm;



    @Size(max = ValidationLimits.PHONE_MAX, message = "전화번호는 20자 이하여야 합니다.")

    private String phoneNumber;



    @NotBlank(message = "닉네임은 필수 입력값입니다.")

    @Size(min = ValidationLimits.NICKNAME_MIN, max = ValidationLimits.NICKNAME_MAX, message = "닉네임은 2~30자여야 합니다.")

    private String nickname;



    @Min(value = ValidationLimits.AGE_MIN, message = "나이 형식이 올바르지 않습니다.")

    @Max(value = ValidationLimits.AGE_MAX, message = "나이 형식이 올바르지 않습니다.")

    private Integer age;

}

