package com.onde.api.application.flight.dto;

import com.onde.core.validation.ValidationLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class FlightSearchRequest {

    @Size(max = 10, message = "출발지 형식이 올바르지 않습니다.")
    private String origin;

    @Size(max = 10, message = "도착지 형식이 올바르지 않습니다.")
    private String destination;

    @Size(max = 10, message = "출발일 형식이 올바르지 않습니다.")
    private String departDate;

    @Min(value = 1, message = "성인 승객 수는 1명 이상이어야 합니다.")
    @Max(value = ValidationLimits.PASSENGER_MAX, message = "성인 승객 수는 9명 이하여야 합니다.")
    private Integer adults;

    @Size(max = 20, message = "tripType 형식이 올바르지 않습니다.")
    private String tripType;

    @Size(max = 200, message = "departures 형식이 올바르지 않습니다.")
    private String departures;

    @Size(max = 200, message = "arrivals 형식이 올바르지 않습니다.")
    private String arrivals;

    @Size(max = 200, message = "dates 형식이 올바르지 않습니다.")
    private String dates;

    @Size(max = 20, message = "seatClass 형식이 올바르지 않습니다.")
    private String seatClass;

    @Builder.Default
    @Min(value = 1, message = "승객 수는 1명 이상이어야 합니다.")
    @Max(value = ValidationLimits.PASSENGER_MAX, message = "승객 수는 9명 이하여야 합니다.")
    private Integer passengerCount = 1;

    // --- 기존 파싱 로직 유지 ---

    public java.util.List<String> getDepartureList() {
        String value = hasText(departures) ? departures : origin;
        if (!hasText(value)) {
            throw new IllegalArgumentException("origin is required");
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .toList();
    }

    public java.util.List<String> getArrivalList() {
        String value = hasText(arrivals) ? arrivals : destination;
        if (!hasText(value)) {
            throw new IllegalArgumentException("destination is required");
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .toList();
    }

    public java.util.List<java.time.LocalDate> getDateList() {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String value = hasText(dates) ? dates : departDate;
        if (!hasText(value)) {
            throw new IllegalArgumentException("departDate is required");
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .map(d -> java.time.LocalDate.parse(d, formatter))
                .toList();
    }

    public Integer getPassengerCount() {
        if (passengerCount != null) {
            return passengerCount;
        }
        if (adults != null) {
            return adults;
        }
        return 1;
    }

    public com.onde.core.entity.flight.SeatClass getParsedSeatClass() {
        if (seatClass == null || seatClass.isBlank()) {
            return null;
        }
        return com.onde.core.entity.flight.SeatClass.valueOf(seatClass.trim().toUpperCase());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
