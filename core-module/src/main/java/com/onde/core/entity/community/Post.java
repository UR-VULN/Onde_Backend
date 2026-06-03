package com.onde.core.entity.community;

import com.onde.core.entity.BaseEntity;
import com.onde.core.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;

/**
 * 커뮤니티 게시글 엔티티입니다.
 * 여행 후기(REVIEW) 및 동행 찾기(COMPANION) 등 피드 콘텐츠를 관리합니다.
 */
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Post extends BaseEntity {

    /**
     * 게시글 고유 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 피드 작성자 회원 ID (FK → members.id)
     */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /**
     * 게시글 제목
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 게시글 본문 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 게시글 유형 (REVIEW / COMPANION)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private PostType type;

    /**
     * 게시글 상태 (ACTIVE / BLINDED / DELETED)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    @Builder.Default
    private PostStatus status = PostStatus.ACTIVE;

    /**
     * 총 좋아요 수 (동시성 업데이트 처리 연동)
     */
    @Column(name = "like_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer likeCount = 0;

    /**
     * 실시간 댓글 수
     */
    @Column(name = "comment_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer commentCount = 0;

    /**
     * 게시글 상태를 변경합니다. (Soft Delete 등)
     */
    public void updateStatus(PostStatus status) {
        this.status = status;
    }

    /**
     * 좋아요 수를 1 증가시킵니다.
     */
    public void incrementLikeCount() {
        if (this.likeCount == null) this.likeCount = 0;
        this.likeCount++;
    }

    /**
     * 좋아요 수를 1 감소시킵니다. (0 이하 방지)
     */
    public void decrementLikeCount() {
        if (this.likeCount != null && this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 댓글 수를 1 증가시킵니다.
     */
    public void incrementCommentCount() {
        if (this.commentCount == null) this.commentCount = 0;
        this.commentCount++;
    }

    /**
     * 댓글 수를 1 감소시킵니다. (0 이하 방지)
     */
    public void decrementCommentCount() {
        if (this.commentCount != null && this.commentCount > 0) {
            this.commentCount--;
        }
    }
}
