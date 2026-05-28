package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.SeatClass;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FlightSearchResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tripType;
    private Integer passengerCount;
    private List<JourneyDto> journeys;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class JourneyDto implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer journeyIndex;
        private String description;
        private List<FlightDto> flights;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class FlightDto implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long scheduleId;
        private String flightNumber;
        private String departureAirport;
        private String arrivalAirport;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private Integer durationMinutes;
        private List<SeatInventoryDto> availableSeats;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class SeatInventoryDto implements Serializable {
        private static final long serialVersionUID = 1L;

        private SeatClass classType;
        private Integer remainingSeats;
        private BigDecimal price;
    }
}
