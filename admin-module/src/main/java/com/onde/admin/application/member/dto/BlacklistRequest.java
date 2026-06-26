package com.onde.admin.application.member.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlacklistRequest {
    @Size(max = 500, message = "블랙리스트 사유는 500자 이하로 입력해야 합니다.")
    private String reason;
}
