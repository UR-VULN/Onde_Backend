package com.onde.api.application.community.dto;

import com.onde.core.entity.community.Post;
import com.onde.core.entity.community.PostType;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {
    private Long postId;
    private String title;
    private PostType type;
    private String authorName;
    private int likeCount;
    private int commentCount;
    private String thumbnailUrl;
    private LocalDateTime createdAt;

    public static PostDto of(Post post, String thumbnailUrl) {
        return PostDto.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .type(post.getType())
                .authorName("User-" + post.getMemberId()) // Member 정보를 가져오지 않으므로 ID로 대체
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0)
                .commentCount(post.getCommentCount() != null ? post.getCommentCount() : 0)
                .thumbnailUrl(thumbnailUrl)
                .createdAt(post.getCreatedAt())
                .build();
    }
}
