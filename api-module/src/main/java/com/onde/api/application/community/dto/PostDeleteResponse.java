package com.onde.api.application.community.dto;

import com.onde.core.entity.community.PostStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDeleteResponse {
    private Long postId;
    private PostStatus status;
}
