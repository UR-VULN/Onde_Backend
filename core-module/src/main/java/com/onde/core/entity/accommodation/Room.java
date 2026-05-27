package com.onde.core.entity.accommodation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoomId")
    private Long roomId;

    // Accommodation과 N:1 관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccommodationId")
    private Accommodation accommodation;

    private String roomType; // 예: 스탠다드, 디럭스, 스위트
    private Integer baseCapacity; // 기준 인원
    private Integer maxCapacity; // 최대 인원
    private Integer defaultPrice; // 기본 요금
    private Integer totalQuantity; // 전체 객실 수
}
