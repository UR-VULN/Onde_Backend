package com.onde.core.repository;

import com.onde.core.entity.accommodation.ApprovalStatus;
import com.onde.core.entity.accommodation.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    @Query("SELECT c FROM Car c " +
           "WHERE c.approvalStatus = :status " +
           "AND (:location IS NULL OR c.location LIKE %:location%) " +
           "AND (:carType IS NULL OR c.carType = :carType)")
    List<Car> searchCars(
            @Param("status") ApprovalStatus status,
            @Param("location") String location,
            @Param("carType") String carType);
}
