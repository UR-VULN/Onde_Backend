package com.onde.core.entity.reservation;

import com.onde.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 10)
    private ReservationTarget targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "check_in")
    private LocalDateTime checkIn;

    @Column(name = "check_out")
    private LocalDateTime checkOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ReservationStatus status;

    @Column(name = "total_price", precision = 19, scale = 2)
    private BigDecimal totalPrice;
}
