package com.onde.core.repository;

import com.onde.core.entity.reservation.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Reservation 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    org.springframework.data.domain.Page<Reservation> findByUserIdAndTargetType(
            Long userId, 
            com.onde.core.entity.reservation.ReservationTarget targetType, 
            org.springframework.data.domain.Pageable pageable
    );

    org.springframework.data.domain.Page<Reservation> findByUserIdAndTargetTypeAndStatus(
            Long userId, 
            com.onde.core.entity.reservation.ReservationTarget targetType, 
            com.onde.core.entity.reservation.ReservationStatus status, 
            org.springframework.data.domain.Pageable pageable
    );

    org.springframework.data.domain.Page<Reservation> findByUserIdAndTargetTypeAndStatusNot(
            Long userId, 
            com.onde.core.entity.reservation.ReservationTarget targetType, 
            com.onde.core.entity.reservation.ReservationStatus status, 
            org.springframework.data.domain.Pageable pageable
    );

    org.springframework.data.domain.Page<Reservation> findByUserIdAndTargetTypeAndStatusIn(
            Long userId, 
            com.onde.core.entity.reservation.ReservationTarget targetType, 
            java.util.Collection<com.onde.core.entity.reservation.ReservationStatus> statuses, 
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(r.totalPrice), 0) FROM Reservation r WHERE r.targetType = :targetType AND r.status != com.onde.core.entity.reservation.ReservationStatus.CANCELLED AND r.createdAt >= :start AND r.createdAt < :end")
    java.math.BigDecimal sumTotalPriceByTargetTypeAndStatusNotAndCreatedAtBetween(
            @org.springframework.data.repository.query.Param("targetType") com.onde.core.entity.reservation.ReservationTarget targetType, 
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, 
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end
    );

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(r) FROM Reservation r WHERE r.status != com.onde.core.entity.reservation.ReservationStatus.CANCELLED AND r.createdAt >= :start AND r.createdAt < :end")
    long countByStatusNotAndCreatedAtBetween(
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, 
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end
    );
}


