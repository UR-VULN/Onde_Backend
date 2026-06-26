package com.onde.api.application.community.dto;

import com.onde.core.entity.community.PostType;
import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = ValidationLimits.TITLE_MAX, message = "제목은 200자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "본문은 필수입니다.")
    @Size(max = ValidationLimits.CONTENT_MAX, message = "본문은 10,000자 이하여야 합니다.")
    private String content;

    @NotNull(message = "게시글 타입은 필수입니다.")
    private PostType type;

    @Min(value = ValidationLimits.RATING_MIN, message = "평점은 1~5 사이여야 합니다.")
    @Max(value = ValidationLimits.RATING_MAX, message = "평점은 1~5 사이여야 합니다.")
    private Integer rating;
}
