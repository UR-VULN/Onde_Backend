package com.onde.core.repository;

import com.onde.core.entity.flight.FlightSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightScheduleRepository extends JpaRepository<FlightSchedule, Long> {

    @Query("SELECT fs FROM FlightSchedule fs JOIN FETCH fs.route r " +
           "WHERE r.departureAirport = :departure " +
           "AND r.arrivalAirport = :arrival " +
           "AND fs.departureTime >= :startTime AND fs.departureTime <= :endTime " +
           "AND fs.status = com.onde.core.entity.flight.ApprovalStatus.APPROVED")
    List<FlightSchedule> findApprovedSchedules(
        @Param("departure") String departure,
        @Param("arrival") String arrival,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    List<FlightSchedule> findByDepartureTimeBetween(LocalDateTime start, LocalDateTime end);
    List<FlightSchedule> findByStatus(com.onde.core.entity.flight.ApprovalStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT fs FROM FlightSchedule fs JOIN FETCH fs.route r " +
           "WHERE r.id = :routeId AND r.sellerId = :sellerId " +
           "AND fs.departureTime >= :start AND fs.departureTime <= :end")
    List<FlightSchedule> findByRouteIdAndSellerIdAndDepartureTimeBetween(
        @org.springframework.data.repository.query.Param("routeId") Long routeId,
        @org.springframework.data.repository.query.Param("sellerId") Long sellerId,
        @org.springframework.data.repository.query.Param("start") LocalDateTime start,
        @org.springframework.data.repository.query.Param("end") LocalDateTime end
    );
}
