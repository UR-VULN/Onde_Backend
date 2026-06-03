package com.onde.api.application.community.dto;

import com.onde.core.entity.community.Post;
import com.onde.core.entity.community.PostStatus;
import com.onde.core.entity.community.PostType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateResponse {
    private Long postId;
    private String title;
    private PostType type;
    private PostStatus status;
    private List<String> imageUrls;
    private LocalDateTime createdAt;

    public static PostCreateResponse of(Post post, List<String> imageUrls) {
        return PostCreateResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .type(post.getType())
                .status(post.getStatus())
                .imageUrls(imageUrls)
                .createdAt(post.getCreatedAt())
                .build();
    }

    // Lombok 미인식 컴파일러 대비 수동 Getter 정의 (Jackson 직렬화 완벽 보장)
    public Long getPostId() {
        return postId;
    }

    public String getTitle() {
        return title;
    }

    public PostType getType() {
        return type;
    }

    public PostStatus getStatus() {
        return status;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
