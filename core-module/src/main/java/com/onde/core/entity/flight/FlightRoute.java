package com.onde.core.entity.flight;

import com.onde.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "flight_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_code", unique = true, nullable = false, length = 20)
    private String routeCode;

    @Column(name = "departure_airport", nullable = false, length = 10)
    private String departureAirport;

    @Column(name = "arrival_airport", nullable = false, length = 10)
    private String arrivalAirport;

    @Column(name = "distance_km", nullable = false)
    private Integer distanceKm;
}
