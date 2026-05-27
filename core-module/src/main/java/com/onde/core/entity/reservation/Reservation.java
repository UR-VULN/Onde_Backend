package com.onde.core.entity.reservation;

import jakarta.persistence.*;
import lombok.*;

/**
 * 예약 정보를 담고 있는 엔티티입니다.
 * 결제 및 정산 대상이 되는 거래의 핵심 단위이며, 특정 판매자(sellerId)와 연계됩니다.
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reservation {

    /**
     * 예약 고유 식별자 (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 예약을 제공한 판매자(호스트/파트너)의 식별자
     * 결제 완료 후 정산 데이터 집계 및 생성 시 이 sellerId를 기준으로 계좌 정보를 조회합니다.
     */
    @Column(name = "seller_id")
    private Long sellerId;
}

