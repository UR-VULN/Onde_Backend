package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.SeatClass;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SellerFlightRegisterRequest {
    private List<FlightItemDto> flights;
    private Long routeId;
    private String flightNumber;
    private String departureAirport;
    private String arrivalAirport;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Integer> operatingDays; // 1:월 ~ 7:일
    private LocalTime departureTime;
    private Integer durationMinutes;
    private List<SeatSetupDto> seatSetup;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class FlightItemDto {
        private String departureAirport;
        private String arrivalAirport;
        private String flightNumber;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private Integer durationMinutes;
        private Map<String, SeatSetupDto> seats;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class SeatSetupDto {
        private SeatClass classType;
        private Integer totalSeats;
        private BigDecimal basePrice;
    }
}
