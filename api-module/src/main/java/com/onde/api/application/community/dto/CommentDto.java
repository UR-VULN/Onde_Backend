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
    private String memberName;
    private String content;
    private LocalDateTime createdAt;

    public static CommentDto of(Comment comment) {
        return CommentDto.builder()
                .commentId(comment.getId())
                .memberName(comment.getMember() != null ? comment.getMember().getName() : "익명")
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
