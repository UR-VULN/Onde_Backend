package com.onde.core.repository;

import com.onde.core.entity.flight.FlightRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FlightRouteRepository extends JpaRepository<FlightRoute, Long> {
    List<FlightRoute> findBySellerId(Long sellerId);
}
