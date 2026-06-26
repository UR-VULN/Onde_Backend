package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.SeatClass;
import com.onde.core.validation.ValidationLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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

    @Valid
    @Size(max = ValidationLimits.LIST_MAX_SIZE, message = "항공편 수가 허용 범위를 초과합니다.")
    private List<FlightItemDto> flights;

    @Min(value = 1, message = "routeId 형식이 올바르지 않습니다.")
    private Long routeId;

    @Size(max = 20, message = "flightNumber는 20자 이하여야 합니다.")
    private String flightNumber;

    @Size(max = 10, message = "공항 코드 형식이 올바르지 않습니다.")
    private String departureAirport;

    @Size(max = 10, message = "공항 코드 형식이 올바르지 않습니다.")
    private String arrivalAirport;

    private LocalDate startDate;
    private LocalDate endDate;

    @Size(max = 7, message = "operatingDays 형식이 올바르지 않습니다.")
    private List<@Min(1) @Max(7) Integer> operatingDays;

    private LocalTime departureTime;

    @Min(value = 1, message = "durationMinutes 형식이 올바르지 않습니다.")
    @Max(value = 1440, message = "durationMinutes 형식이 올바르지 않습니다.")
    private Integer durationMinutes;

    @Valid
    @Size(max = 10, message = "좌석 설정 수가 허용 범위를 초과합니다.")
    private List<SeatSetupDto> seatSetup;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class FlightItemDto {

        @Size(max = 10, message = "공항 코드 형식이 올바르지 않습니다.")
        private String departureAirport;

        @Size(max = 10, message = "공항 코드 형식이 올바르지 않습니다.")
        private String arrivalAirport;

        @Size(max = 20, message = "flightNumber는 20자 이하여야 합니다.")
        private String flightNumber;

        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;

        @Min(value = 1, message = "durationMinutes 형식이 올바르지 않습니다.")
        @Max(value = 1440, message = "durationMinutes 형식이 올바르지 않습니다.")
        private Integer durationMinutes;

        @Valid
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

        @Min(value = 0, message = "좌석 수는 0 이상이어야 합니다.")
        @Max(value = 9999, message = "좌석 수가 허용 범위를 초과합니다.")
        private Integer totalSeats;

        @DecimalMin(value = "0", message = "기본 가격은 0원 이상이어야 합니다.")
        @DecimalMax(value = "999999999", message = "기본 가격이 허용 범위를 초과합니다.")
        private BigDecimal basePrice;
    }
}
