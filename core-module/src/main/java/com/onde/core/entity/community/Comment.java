package com.onde.core.entity.community;

import com.onde.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 커뮤니티 댓글 엔티티입니다.
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_secret", nullable = false)
    @Builder.Default
    private Boolean isSecret = false;

    public void updateContent(String content, Boolean isSecret) {
        this.content = content;
        this.isSecret = isSecret;
    }
}
