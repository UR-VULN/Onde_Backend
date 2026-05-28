package com.onde.core.entity.flight;

import com.onde.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "flight_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightBooking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_code", unique = true, nullable = false, length = 30)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_schedule_id", foreignKey = @ForeignKey(name = "fk_booking_schedule"))
    private FlightSchedule flightSchedule;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Embedded
    private Passenger passenger;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_class", nullable = false, length = 15)
    private SeatClass seatClass;

    @Column(name = "total_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.RESERVED;

    @Column(name = "reserved_until", nullable = false)
    private LocalDateTime reservedUntil;
}
