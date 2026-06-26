package com.onde.api.application.community.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequest {

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = ValidationLimits.COMMENT_MAX, message = "댓글은 2,000자 이하여야 합니다.")
    private String content;

    private Boolean isSecret;
}
