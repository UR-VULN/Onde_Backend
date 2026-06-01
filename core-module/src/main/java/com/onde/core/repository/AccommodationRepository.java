package com.onde.core.repository;

import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {
    @Query("SELECT DISTINCT a FROM Accommodation a " +
           "LEFT JOIN Room r ON r.accommodation = a " +
           "WHERE a.approvalStatus = :status " +
           "AND (:location IS NULL OR a.location LIKE %:location%) " +
           "AND (:category IS NULL OR a.category = :category) " +
           "AND (:checkIn IS NULL OR :checkOut IS NULL OR EXISTS (" +
           "    SELECT i FROM Inventory i " +
           "    WHERE i.targetType = 'ROOM' AND i.targetId = r.id " +
           "    AND i.date BETWEEN :checkIn AND :checkOut " +
           "    AND i.stock > 0 " +
           "    GROUP BY i.targetId " +
           "    HAVING COUNT(i) = :days))")
    List<Accommodation> searchAccommodations(
            @Param("status") ApprovalStatus status,
            @Param("location") String location,
            @Param("category") String category,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("days") Long days,
            Sort sort);
            
       List<Accommodation> findByApprovalStatus(ApprovalStatus status);
       List<Accommodation> findBySellerId(Long sellerId);
}
