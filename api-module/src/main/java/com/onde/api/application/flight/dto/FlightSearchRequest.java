package com.onde.api.application.flight.dto;

import com.onde.core.entity.flight.SeatClass;
import lombok.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class FlightSearchRequest {

    private String origin;         // PDF spec: ICN
    private String destination;    // PDF spec: NRT
    private String departDate;     // PDF spec: 2026-06-01
    private Integer adults;        // PDF spec: 1+

    private String tripType;       // ONE_WAY, ROUND_TRIP, MULTI_CITY
    private String departures;     // ICN,NRT
    private String arrivals;       // SFO,ICN
    private String dates;          // 2026-07-01,2026-07-10
    private String seatClass;      // FIRST, BUSINESS, ECONOMY (Optional)
    @Builder.Default
    private Integer passengerCount = 1; // (Optional)

    public List<String> getDepartureList() {
        String value = hasText(departures) ? departures : origin;
        if (!hasText(value)) {
            throw new IllegalArgumentException("origin is required");
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public List<String> getArrivalList() {
        String value = hasText(arrivals) ? arrivals : destination;
        if (!hasText(value)) {
            throw new IllegalArgumentException("destination is required");
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public List<LocalDate> getDateList() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String value = hasText(dates) ? dates : departDate;
        if (!hasText(value)) {
            throw new IllegalArgumentException("departDate is required");
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .map(d -> LocalDate.parse(d, formatter))
                .collect(Collectors.toList());
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

    public SeatClass getParsedSeatClass() {
        if (seatClass == null || seatClass.isBlank()) {
            return null;
        }
        return SeatClass.valueOf(seatClass.trim().toUpperCase());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
