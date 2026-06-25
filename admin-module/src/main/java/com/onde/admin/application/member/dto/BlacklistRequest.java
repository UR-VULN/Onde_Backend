package com.onde.admin.application.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlacklistRequest {
    @NotBlank(message = "블랙리스트 사유는 필수입니다.")
    @Size(max = 100, message = "블랙리스트 사유는 최대 100자까지 입력 가능합니다.")
    private String reason;
}
