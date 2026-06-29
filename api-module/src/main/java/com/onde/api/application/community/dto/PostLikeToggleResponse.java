package com.onde.api.application.community.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLikeToggleResponse {
    private Long postId;
    private boolean isLiked;
    private int likeCount;
}
