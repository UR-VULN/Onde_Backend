package com.onde.core.entity.accommodation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Car {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CarId")
    private Long carId;

    @Column(name = "SellerId")
    private Long sellerId;

    private String modelName; // 예: 아반떼, 쏘렌토
    private String carType; // 경형, 세단, SUV

    private String location; // 렌터카 대여/반납 장소(검색용)

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer totalQuantity;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;
}
