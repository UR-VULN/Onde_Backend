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

    private String tripType;       // ONE_WAY, ROUND_TRIP, MULTI_CITY
    private String departures;     // ICN,NRT
    private String arrivals;       // SFO,ICN
    private String dates;          // 2026-07-01,2026-07-10
    private String seatClass;      // FIRST, BUSINESS, ECONOMY (Optional)
    @Builder.Default
    private Integer passengerCount = 1; // (Optional)

    public List<String> getDepartureList() {
        return Arrays.stream(departures.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public List<String> getArrivalList() {
        return Arrays.stream(arrivals.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public List<LocalDate> getDateList() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return Arrays.stream(dates.split(","))
                .map(String::trim)
                .map(d -> LocalDate.parse(d, formatter))
                .collect(Collectors.toList());
    }

    public SeatClass getParsedSeatClass() {
        if (seatClass == null || seatClass.isBlank()) {
            return null;
        }
        return SeatClass.valueOf(seatClass.trim().toUpperCase());
    }
}
