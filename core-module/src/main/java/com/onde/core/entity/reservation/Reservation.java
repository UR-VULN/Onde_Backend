package com.onde.core.entity.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private ReservationTarget targetType; // STAY, CAR, FLIGHT

    private Long targetId; // RoomId, CarId, etc.

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private Integer totalPrice;
    private LocalDateTime createdAt;
}
