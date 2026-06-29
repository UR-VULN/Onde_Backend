package com.onde.core.repository;

import com.onde.core.entity.lbs.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    @Query("""
        SELECT p FROM Property p
        WHERE p.isVerified = true
          AND p.latitude BETWEEN :swLat AND :neLat
          AND p.longitude BETWEEN :swLng AND :neLng
    """)
    List<Property> findVerifiedByBoundingBox(
        @Param("swLat") Double swLat,
        @Param("swLng") Double swLng,
        @Param("neLat") Double neLat,
        @Param("neLng") Double neLng
    );

    long countByIsVerified(Boolean isVerified);
}

