package com.onde.core.entity.lbs;

import com.onde.core.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 논리 FK 형태로 DB 물리 외래키 제약조건 제거
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member seller;

    @Column(name = "address_name", nullable = false, length = 500)
    private String addressName;

    // 구글 맵 UI 연동용 위도, 경도 좌표 (소수점 4자리 이상 정밀도)
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;
}
