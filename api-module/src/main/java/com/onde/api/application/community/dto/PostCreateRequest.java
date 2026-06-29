package com.onde.api.application.community.dto;

import com.onde.core.entity.community.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @jakarta.validation.constraints.Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "본문은 필수입니다.")
    @jakarta.validation.constraints.Size(max = 2000, message = "본문은 2000자를 초과할 수 없습니다.")
    private String content;

    @NotNull(message = "게시글 타입은 필수입니다.")
    private PostType type;

    private Integer rating;
}
