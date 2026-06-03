package com.onde.core.entity.community;

import com.onde.core.entity.BaseEntity;
import com.onde.core.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;

/**
 * 게시글 댓글 엔티티입니다.
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    /**
     * 댓글 고유 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 댓글이 달린 대상 게시글 ID (FK → posts.id Physical)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /**
     * 댓글 작성자 회원 ID (FK → members.id Physical)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 댓글 본문 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
