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
    @Column(name = "target_type", nullable = false, length = 10)
    private ReservationTarget targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "stock", nullable = false)
    private Integer stock;
}
