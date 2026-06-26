package com.onde.api.application.community.dto;

import com.onde.core.entity.community.Comment;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long commentId;
    private Long postId;
    private String authorName;
    private String content;
    private Boolean isSecret;
    private Boolean isMine;
    private LocalDateTime createdAt;

    public static CommentDto of(Comment comment, String authorName, String displayContent, boolean isMine) {
        return CommentDto.builder()
                .commentId(comment.getId())
                .postId(comment.getPostId())
                .authorName(authorName)
                .content(displayContent)
                .isSecret(comment.getIsSecret())
                .isMine(isMine)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
