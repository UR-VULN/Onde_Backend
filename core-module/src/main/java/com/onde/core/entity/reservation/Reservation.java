package com.onde.core.entity.reservation;

import com.onde.core.entity.BaseEntity;
import com.onde.core.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 논리 FK 형태로 DB 물리 외래키 제약조건 제거
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @Column(name = "product_name", nullable = false, length = 300)
    private String productName;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "mileage_used", nullable = false)
    private Integer mileageUsed;

    @Column(name = "reservation_date", nullable = false)
    private LocalDateTime reservationDate;
}
