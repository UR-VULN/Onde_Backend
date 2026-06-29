package com.onde.admin.application.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBlindRequest {

    @NotBlank(message = "블라인드 사유는 필수입니다.")
    @Size(max = 500, message = "블라인드 사유는 500자를 초과할 수 없습니다.")
    private String reason;
}
