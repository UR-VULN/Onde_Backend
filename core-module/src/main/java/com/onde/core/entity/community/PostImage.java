package com.onde.core.entity.community;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 게시글 다중 첨부 이미지 (최대 3장) 엔티티입니다.
 */
@Entity
@Table(name = "post_images")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostImage {

    /**
     * 이미지 고유 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이미지가 속한 게시글 ID (FK → posts.id)
     */
    @Column(name = "post_id", nullable = false)
    private Long postId;

    /**
     * AWS S3 업로드 후 발급된 CloudFront CDN 주소 URL
     */
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /**
     * 파일 업로드 완료 일시
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false, columnDefinition = "DATETIME DEFAULT NOW()")
    private LocalDateTime createdAt;

    /**
     * 다중 이미지의 노출 정렬 순서 (0, 1, 2)
     */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
