package com.onde.core.entity.lbs;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 가이드 마커 엔티티입니다.
 * 지도 UI에 표시될 관광지, 맛집 등의 정보를 관리합니다.
 */
@Entity
@Table(name = "guide_markers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuideMarker {

    /**
     * 마커 고유 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "marker_id")
    private Long markerId;

    /**
     * 지도 UI에 표시될 관광지·맛집 상호명
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 마커 카테고리 (RESTAURANT / ATTRACTION / CAFE / etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private MarkerCategory category;

    /**
     * Google Maps 클릭으로 수집한 정밀 위도 좌표
     */
    @Column(nullable = false)
    private Double latitude;

    /**
     * Google Maps 클릭으로 수집한 정밀 경도 좌표
     */
    @Column(nullable = false)
    private Double longitude;

    /**
     * 등록을 수행한 본사 어드민 관리자 ID 또는 사번
     */
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    /**
     * 마커 최초 생성 및 등록 일시
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false, columnDefinition = "DATETIME DEFAULT NOW()")
    private LocalDateTime createdAt;
}
