package com.onde.core.entity.lbs;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "properties")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Property {

    /**
     * 매물 위치 고유 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 해당 매물의 소유 판매자 (FK → members.id 논리)
     */
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    /**
     * Places Autocomplete를 통해 수집된 주소 문자열
     */
    @Column(name = "address_name", nullable = false, length = 500)
    private String addressName;

    /**
     * 구글 맵 UI 연동용 위도 좌표
     */
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    /**
     * 구글 맵 UI 연동용 경도 좌표
     */
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    /**
     * 본사 검증 완료 여부 (사용자 화면 지도 노출 필터 키)
     */
    @Column(name = "is_verified", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * 매물 등록 일시
     */
    @CreatedDate
    @Column(name = "registered_at", updatable = false, columnDefinition = "DATETIME DEFAULT NOW()")
    private LocalDateTime registeredAt;
}
