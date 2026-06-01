package com.onde.core.repository;

import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.accommodation.Car;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    @Query("SELECT c FROM Car c " +
           "WHERE c.approvalStatus = :status " +
           "AND (:carType IS NULL OR c.carType = :carType) " +
           "AND (:startDate IS NULL OR :endDate IS NULL OR EXISTS (" +
           "    SELECT i FROM Inventory i " +
           "    WHERE i.targetType = 'CAR' AND i.targetId = c.id " +
           "    AND i.date BETWEEN :startDate AND :endDate " +
           "    AND i.stock > 0 " +
           "    GROUP BY i.targetId " +
           "    HAVING COUNT(i) = :days))")
    List<Car> searchCars(
            @Param("status") ApprovalStatus status,
            @Param("carType") String carType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("days") Long days,
            Sort sort);
            
       List<Car> findByApprovalStatus(ApprovalStatus status);
       List<Car> findBySellerId(Long sellerId);
}

