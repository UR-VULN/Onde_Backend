package com.onde.core.entity.accommodation;

import com.onde.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rental_cars")
@Getter @Setter @NoArgsConstructor
public class Car extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "car_id")
    private Long id;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "car_type")
    private String carType;

    @Column(name = "fuel_type")
    private String fuelType;

    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalStatus approvalStatus;
}
