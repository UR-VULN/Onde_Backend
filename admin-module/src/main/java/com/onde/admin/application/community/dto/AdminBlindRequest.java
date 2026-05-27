package com.onde.admin.application.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBlindRequest {

    @NotBlank(message = "블라인드 사유는 필수입니다.")
    private String reason;
}
