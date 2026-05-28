package com.onde.core.repository;

import com.onde.core.entity.flight.BookingStatus;
import com.onde.core.entity.flight.FlightBooking;
import com.onde.core.entity.flight.SeatClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightBookingRepository extends JpaRepository<FlightBooking, Long> {
    List<FlightBooking> findByStatusAndReservedUntilBefore(BookingStatus status, LocalDateTime dateTime);

    @Query("SELECT COUNT(fb) FROM FlightBooking fb WHERE fb.flightSchedule.id = :scheduleId AND fb.seatClass = :seatClass AND fb.status IN (com.onde.core.entity.flight.BookingStatus.CONFIRMED, com.onde.core.entity.flight.BookingStatus.RESERVED)")
    long countActiveBookings(
        @Param("scheduleId") Long scheduleId,
        @Param("seatClass") SeatClass seatClass
    );

    @org.springframework.data.jpa.repository.QueryHints(value = {
        @jakarta.persistence.QueryHint(name = "org.hibernate.fetchSize", value = "100"),
        @jakarta.persistence.QueryHint(name = "org.hibernate.cacheable", value = "false")
    })
    @Query("SELECT fb FROM FlightBooking fb WHERE fb.flightSchedule.id = :scheduleId")
    java.util.stream.Stream<FlightBooking> streamByFlightScheduleId(@Param("scheduleId") Long scheduleId);
}
