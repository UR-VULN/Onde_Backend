package com.onde.core.entity.community;

import jakarta.persistence.*;
import lombok.*;

/**
 * 게시글 좋아요 관계 엔티티입니다.
 */
@Entity
@Table(
    name = "post_likes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"post_id", "member_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostLike {

    /**
     * 좋아요 관계 고유 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 좋아요 대상 게시글 ID (FK → posts.id)
     */
    @Column(name = "post_id", nullable = false)
    private Long postId;

    /**
     * 좋아요를 클릭한 회원 ID (FK → members.id)
     */
    @Column(name = "member_id", nullable = false)
    private Long memberId;
}
