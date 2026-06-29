package com.onde.admin.application.community.dto;

import com.onde.core.entity.community.PostStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBlindResponse {
    private Long postId;
    private PostStatus status;
    private LocalDateTime blindedAt;
}
