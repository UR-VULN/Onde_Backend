package com.onde.api.application.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SensitiveRevealPasswordRequest {

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;
}
