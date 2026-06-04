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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false, referencedColumnName = "id")
    private Accommodation accommodation;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "base_capacity", nullable = false)
    private Integer baseCapacity = 2;

    @Column(name = "surcharge_per_person", nullable = false)
    private BigDecimal surchargePerPerson = BigDecimal.valueOf(20000);
}
