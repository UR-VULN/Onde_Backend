package com.onde.core.repository;

import com.onde.core.entity.flight.FlightRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightRouteRepository extends JpaRepository<FlightRoute, Long> {
}
