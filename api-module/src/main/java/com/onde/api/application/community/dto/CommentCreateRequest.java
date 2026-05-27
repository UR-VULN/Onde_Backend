package com.onde.api.application.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentCreateRequest {

    @NotBlank(message = "댓글 내용은 필수 입력 값입니다.")
    private String content;
}
