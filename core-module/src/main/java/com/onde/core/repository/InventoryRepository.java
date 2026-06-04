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
       List<Inventory> findByTargetTypeAndTargetIdAndDateBetween(ReservationTarget targetType, Long targetId,
                     LocalDate startDate, LocalDate endDate);

       Optional<Inventory> findByTargetTypeAndTargetIdAndDate(ReservationTarget targetType, Long targetId,
                     LocalDate date);

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

       /**
        * 숙소 ID 목록에 해당하는 객실의 오늘 이후 최저가를 한 번에 Bulk 조회 (N+1 방지).
        * 반환: [accommodation_id, min_price] 형태의 Object[]
        */
       @Query(value = "SELECT r.accommodation_id, MIN(i.base_price) " +
                     "FROM inventory i " +
                     "JOIN rooms r ON r.id = i.target_id AND i.target_type = 'ROOM' " +
                     "WHERE r.accommodation_id IN :accommodationIds " +
                     "  AND i.stock > 0 " +
                     "  AND i.date >= CURDATE() " +
                     "GROUP BY r.accommodation_id", nativeQuery = true)
       List<Object[]> findMinPriceByAccommodationIds(@Param("accommodationIds") List<Long> accommodationIds);
}
