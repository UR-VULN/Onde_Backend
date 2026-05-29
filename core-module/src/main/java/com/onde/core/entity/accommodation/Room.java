package com.onde.core.entity.accommodation;

import com.onde.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
@Getter @Setter @NoArgsConstructor
public class Room extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id")
    private Accommodation accommodation;

    private String name;

    private Integer capacity;

    @Column(name = "base_capacity")
    private Integer baseCapacity;

    @Column(name = "extra_person_fee", precision = 19, scale = 2)
    private BigDecimal extraPersonFee;
}
