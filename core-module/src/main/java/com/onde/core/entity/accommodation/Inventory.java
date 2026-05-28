package com.onde.core.entity.accommodation;

import com.onde.core.entity.BaseEntity;
import com.onde.core.entity.reservation.ReservationTarget;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inventory")
@Getter @Setter @NoArgsConstructor
public class Inventory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 10)
    private ReservationTarget targetType;

    @Column(name = "target_id")
    private Long targetId;

    private LocalDate date;

    @Column(name = "base_price", precision = 19, scale = 2)
    private BigDecimal basePrice;

    private Integer stock;
}
