package com.onde.core.entity.flight;

import com.onde.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "seat_inventories",
       uniqueConstraints = {@UniqueConstraint(name = "uq_schedule_class", columnNames = {"flight_schedule_id", "class_type"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatInventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_schedule_id", nullable = false)
    private Long flightScheduleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_type", nullable = false, length = 15)
    private SeatClass classType; // FIRST, BUSINESS, ECONOMY

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "remaining_seats", nullable = false, columnDefinition = "INT CHECK (remaining_seats >= 0)")
    private Integer remainingSeats;

    @Column(name = "base_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal basePrice;
}
