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
}

