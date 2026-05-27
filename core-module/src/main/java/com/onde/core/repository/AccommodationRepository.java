package com.onde.core.repository;

import com.onde.core.entity.accommodation.Accommodation;
import com.onde.core.entity.accommodation.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {
    @Query("SELECT DISTINCT a FROM Accommodation a " +
           "WHERE a.approvalStatus = :status " +
           "AND (:region IS NULL OR a.region LIKE %:region%) " +
           "AND (:starRating IS NULL OR a.starRating >= :starRating)")
    List<Accommodation> searchAccommodations(
            @Param("status") ApprovalStatus status,
            @Param("region") String region,
            @Param("starRating") Integer starRating);
}
