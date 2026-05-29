package com.onde.core.repository;

import com.onde.core.entity.accommodation.Inventory;
import com.onde.core.entity.reservation.ReservationTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByTargetTypeAndTargetIdAndDateBetween(ReservationTarget targetType, Long targetId, LocalDate startDate, LocalDate endDate);
    Optional<Inventory> findByTargetTypeAndTargetIdAndDate(ReservationTarget targetType, Long targetId, LocalDate date);

    @Query("SELECT COUNT(i) FROM Inventory i " +
           "WHERE i.targetType = :targetType " +
           "AND i.targetId = :targetId " +
           "AND i.date BETWEEN :startDate AND :endDate " +
           "AND i.stock > 0")
    long countAvailableDays(
            @Param("targetType") ReservationTarget targetType,
            @Param("targetId") Long targetId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
