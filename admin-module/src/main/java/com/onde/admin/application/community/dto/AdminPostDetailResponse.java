package com.onde.admin.application.community.dto;

import com.onde.core.entity.community.Post;
import com.onde.core.entity.community.PostStatus;
import com.onde.core.entity.community.PostType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPostDetailResponse {
    private Long postId;
    private Long memberId;
    private String authorName;
    private String title;
    private String content;
    private PostType type;
    private PostStatus status;
    private int likeCount;
    private int commentCount;
    private Integer rating;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminPostDetailResponse of(Post post, List<String> imageUrls, String authorName) {
        return AdminPostDetailResponse.builder()
                .postId(post.getId())
                .memberId(post.getMemberId())
                .authorName(authorName)
                .title(post.getTitle())
                .content(post.getContent())
                .type(post.getType())
                .status(post.getStatus())
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0)
                .commentCount(post.getCommentCount() != null ? post.getCommentCount() : 0)
                .rating(post.getRating())
                .imageUrls(imageUrls)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
